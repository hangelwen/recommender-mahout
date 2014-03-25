package FacebookLikeRecommender;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.model.DataModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class LikesRecommenderServlet extends HttpServlet {

	class RequestException extends Exception {
		public RequestException(String message) {
			super(message);
		}
	}

	private static final int NUM_TOP_PREFERENCES = 20;
	private static final int DEFAULT_HOW_MANY = 20;	
	private static final Logger log = 
			LoggerFactory.getLogger(LikesRecommenderServlet.class);
	private UserBasedAnonymousRecommender recommender;

	private void validateRequest(HttpServletRequest request) 
			throws RequestException {
		if (request.getParameter("recommend") == null) {
			throw new RequestException
			("Recommendation type not specified! (Please use eitheir "
					+ "'recommend=likes' or 'recommend=users').");
		}
		String recommendType = request.getParameter("recommend");
		if (recommendType.compareTo("likes") !=0 &&
				recommendType.compareTo("users") !=0) {
			throw new RequestException
			("Wrong recommendation type! (Please use eitheir "
					+ "'recommend=likes' or 'recommend=users').");
		}
		if (recommendType.compareTo("likes") == 0) {
			if (request.getParameter("likes") == null) {
				throw new RequestException
				("Recommendation type is 'likes' but no likes specified! "
						+ "Please input a list of likes, seperated by comma.");
			}
			String likes = request.getParameter("likes");
			if (likes.trim().length() == 0) {
				throw new RequestException
				("Recommendation type is 'likes' but no likes specified! "
						+ "Please input a list of likes, seperated by comma.");
			}
		}
		if (recommendType.compareTo("users") == 0) {
			if (request.getParameter("likes") == null && 
					request.getParameter("users") == null) {
				throw new RequestException
				("Recommentation type is 'users' but no users or likes "
						+ "specified! Please input a list of likes/userids, "
						+ "seperated by comma.");
			}
			if (request.getParameter("likes") != null) {
				String likes = request.getParameter("likes");
				if (likes.trim().length() == 0) {
					throw new RequestException
					("Recommendation type is 'users' but no likes specified! "
							+ "Please input a list of likes, seperated by comma.");
				}
			}
			if (request.getParameter("users") != null) {
				String likes = request.getParameter("users");
				if (likes.trim().length() == 0) {
					throw new RequestException
					("Recommendation type is 'users' but no users specified! "
							+ "Please input a list of users, seperated by comma.");
				}
			}
		}
	}
	@Override
	public void init(ServletConfig config) throws ServletException {
		log.info("Initializing recommender servlet!!");
		super.init(config);
		String recommenderClassName = config.getInitParameter("recommender-class");
		if (recommenderClassName == null) {
			throw new ServletException("Servlet init-param \"recommender-class\" is not defined");
		}
		UserRecommenderSingleton.initializeIfNeeded(recommenderClassName);
		recommender = UserRecommenderSingleton.getInstance().getRecommender();
		log.info("Finished initializing recommender!!");
	}

	@Override
	public void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		try {
			validateRequest(request);
			if (request.getParameter("recommend").compareTo("likes") == 0) {
				String likes = request.getParameter("likes");	
				String [] likeArray = likes.trim().split(",");
				String howManyString = request.getParameter("howMany");
				int howMany = howManyString == null ? DEFAULT_HOW_MANY : Integer.parseInt(howManyString);
				boolean debug = Boolean.parseBoolean(request.getParameter("debug"));
				String format = request.getParameter("format");
				if (format == null) {
					format = "text";
				}
				List<String> items = recommender.recommendLikesForAnonymous(likeArray, howMany);
				if ("text".equals(format)) {
					writePlainText(response, debug, items);
				} else if ("xml".equals(format)) {
					writeXML(response, items);
				} else if ("json".equals(format)) {
					writeJSON(response, items);
				} else {
					throw new ServletException("Bad format parameter: " + format);
				}
			}
			else {
				String howManyString = request.getParameter("howMany");
				int howMany = howManyString == null ? DEFAULT_HOW_MANY : Integer.parseInt(howManyString);
				boolean debug = Boolean.parseBoolean(request.getParameter("debug"));
				String format = request.getParameter("format");
				if (format == null) {
					format = "text";
				}
				// input are userIDs, find similar users for each userID.
				if (request.getParameter("users") != null) {  
					String users = request.getParameter("users");	
					String [] userArray = users.trim().split(",");
					List<String> ret = recommender.recommendUsersForUsers(userArray, howMany);
					if ("text".equals(format)) {
						writePlainText(response, debug, ret);
					} else if ("xml".equals(format)) {
						writeXML(response, ret);
					} else if ("json".equals(format)) {
						writeJSON(response, ret);
					} else {
						throw new ServletException("Bad format parameter: " + format);
					}
				}
				else {  // input are likes, recommend for a new user.
					String likes = request.getParameter("likes");	
					String [] likeArray = likes.trim().split(",");
					List<String> users = recommender.recommendUsersForAnonymous(likeArray, howMany);
					if ("text".equals(format)) {
						writePlainText(response, debug, users);
					} else if ("xml".equals(format)) {
						writeXML(response, users);
					} else if ("json".equals(format)) {
						writeJSON(response, users);
					} else {
						throw new ServletException("Bad format parameter: " + format);
					}
				}
			}
		} catch (TasteException te) {
			throw new ServletException(te);
		} catch (IOException ioe) {
			throw new ServletException(ioe);
		} catch (RequestException e) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
		}
	}

	private static void writeXML(HttpServletResponse response, Iterable<String> items) throws IOException {
		response.setContentType("application/xml");
		response.setCharacterEncoding("UTF-8");
		response.setHeader("Cache-Control", "no-cache");
		PrintWriter writer = response.getWriter();
		writer.print("<?xml version=\"1.0\" encoding=\"UTF-8\"?><recommendedItems>");
		for (String recommendedItem : items) {
			writer.print("<item>");
			writer.print(recommendedItem);
		}
		writer.println("</recommendedItems>");
	}

	private static void writeJSON(HttpServletResponse response, Iterable<String> items) throws IOException {
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		response.setHeader("Cache-Control", "no-cache");
		PrintWriter writer = response.getWriter();
		writer.print("{\"recommendedItems\":{\"item\":[");
		for (String recommendedItem : items) {
			writer.print("{\"value\":\"");
			writer.print(recommendedItem);
			writer.print("\"},");
		}
		writer.println("]}}");
	}
	private void writePlainText(HttpServletResponse response,
			boolean debug,
			Iterable<String> items) throws IOException, TasteException {
		response.setContentType("text/plain");
		response.setCharacterEncoding("UTF-8");
		response.setHeader("Cache-Control", "no-cache");
		PrintWriter writer = response.getWriter();
		if (debug) {
			writeDebugRecommendations(items, writer);
		} else {
			writeRecommendations(items, writer);
		}
	}

	private static void writeRecommendations(Iterable<String> items, PrintWriter writer) {
		for (String recommendedItem : items) {
			writer.println(recommendedItem);
		}
	}


	private void writeDebugRecommendations(Iterable<String> items, PrintWriter writer)
			throws TasteException {
		DataModel dataModel = recommender.getDataModel();
		writer.print("Recommender: ");
		writer.println(recommender);
		writer.println();
		writer.print("Top ");
		writer.print(NUM_TOP_PREFERENCES);
		writer.println(" Preferences:");
		writer.println();
		writer.println("Recommendations:");
		writer.println();
		for (String recommendedItem : items) {
			writer.println(recommendedItem);
		}
	}
}
