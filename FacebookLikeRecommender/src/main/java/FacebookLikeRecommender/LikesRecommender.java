package FacebookLikeRecommender;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.PriorityQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.mahout.cf.taste.common.Refreshable;
import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.impl.neighborhood.NearestNUserNeighborhood;
import org.apache.mahout.cf.taste.impl.recommender.GenericBooleanPrefUserBasedRecommender;
import org.apache.mahout.cf.taste.impl.similarity.LogLikelihoodSimilarity;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.model.Preference;
import org.apache.mahout.cf.taste.model.PreferenceArray;
import org.apache.mahout.cf.taste.impl.common.FastByIDMap;
import org.apache.mahout.cf.taste.impl.model.BooleanPreference;
import org.apache.mahout.cf.taste.impl.model.BooleanUserPreferenceArray;
import org.apache.mahout.cf.taste.impl.model.GenericBooleanPrefDataModel;
import org.apache.mahout.cf.taste.impl.model.MemoryIDMigrator;
import org.apache.mahout.cf.taste.impl.model.PlusAnonymousUserDataModel;
import org.apache.mahout.cf.taste.recommender.IDRescorer;
import org.apache.mahout.cf.taste.recommender.RecommendedItem;
import org.apache.mahout.cf.taste.recommender.Rescorer;
import org.apache.mahout.cf.taste.similarity.UserSimilarity;
import org.apache.mahout.common.LongPair;

import java.util.Comparator;

public class LikesRecommender implements UserBasedAnonymousRecommender {

	public class SimilarUserPair {
		private double similarity;
		long [] userIDs;

		public SimilarUserPair(long user1, long user2, double similarity) {
			this.userIDs = new long[2];
			this.userIDs[0] = user1;
			this.userIDs[1] = user2;
			this.similarity = similarity;
		}

		public long [] getUserIDs () {
			return userIDs;
		}

		public long getUser1ID() {
			return userIDs[0];
		}

		public long getUser2ID() {
			return userIDs[1];
		}

		public double getSimilarity() {
			return this.similarity;
		}
	}

	public class UserSimilarityComparator implements Comparator<SimilarUserPair>
	{
		public int compare(SimilarUserPair x, SimilarUserPair y)
		{
			if (x.similarity < y.similarity)
			{
				return -1;
			}
			if (x.similarity > y.similarity)
			{
				return 1;
			}
			return 0;
		}
	}

	private GenericBooleanPrefUserBasedRecommender delegate;
	private DataModel dataModel;
	private PlusAnonymousUserDataModel plusModel;
	private  String DATA_FILE_NAME = "small.dat";

	//keep the mapping between usernames/likes and their long IDs
	private MemoryIDMigrator thing2long = new MemoryIDMigrator();

	private final Logger logger = 
			LoggerFactory.getLogger(LikesRecommender.class);

	private void initRecommender() throws TasteException {
		try {
			Map<Long,List<Preference>> preferecesOfUsers = 
					new HashMap<Long,List<Preference>>();
			URL url = getClass().getClassLoader().getResource(DATA_FILE_NAME);
			// create a file out of the resource
			File data = new File(url.toURI());
			//File data = new File("/tmp/large.csv");
			logger.info("Starting importing data.");
			BufferedReader br = new BufferedReader(new FileReader(data));

			String line;
			while ((line = br.readLine()) != null) {

				String[] sp = line.trim().split("\\t");
				long userLong = thing2long.toLongID(sp[0]);
				// store the mapping for the user
				thing2long.storeMapping(userLong, sp[0]);
				long itemLong = thing2long.toLongID(sp[1]);
				// store the mapping for the item
				thing2long.storeMapping(itemLong, sp[1]);

				List<Preference> userPrefList;
				if ((userPrefList = preferecesOfUsers.get(userLong)) == null) {
					userPrefList = new ArrayList<Preference>();
					preferecesOfUsers.put(userLong, userPrefList);
				}	
				// add the like that we just found to this user
				userPrefList.add(new BooleanPreference(userLong, itemLong));
				//logger.info("Adding "+sp[0]+"("+userLong+") to "+sp[1]+"("+itemLong+")");
			}
			br.close();

			// create the corresponding mahout data structure from the map
			FastByIDMap<PreferenceArray> preferecesOfUsersFastMap = 
					new FastByIDMap<PreferenceArray>();
			for(Entry<Long, List<Preference>> entry : 
				preferecesOfUsers.entrySet()) {
				preferecesOfUsersFastMap.put(entry.getKey(), 
						new BooleanUserPreferenceArray(entry.getValue()));
			}
			logger.info("Data loaded.");
			// create a data model
			dataModel = new GenericBooleanPrefDataModel(
					GenericBooleanPrefDataModel.toDataMap(preferecesOfUsersFastMap));
			//plusModel = new PlusAnonymousConcurrentUserDataModel(dataModel, this.DEFAULT_CONCURRENT_USER);
			plusModel = new PlusAnonymousUserDataModel(dataModel);
			// Instantiate the recommender
			UserSimilarity similarity = new LogLikelihoodSimilarity(plusModel);
			NearestNUserNeighborhood neighborhood = 
					new NearestNUserNeighborhood(50, similarity, plusModel);
			delegate = new GenericBooleanPrefUserBasedRecommender(
					plusModel, neighborhood, similarity);
		} catch (FileNotFoundException e) {
			logger.error("Data file was not found", e);
		} catch (IOException e) {
			logger.error("Error during reading line of file", e);
		}
		catch (URISyntaxException e) {
			logger.error("Problem with the file URL", e);
		}
	}

	public LikesRecommender () throws TasteException, IOException {
		this.initRecommender();
	}


	public void refresh(Collection<Refreshable> alreadyRefreshed) {
		delegate.refresh(alreadyRefreshed);

	}

	public float estimatePreference(long userID, long itemID) 
			throws TasteException {
		return delegate.estimatePreference(userID, itemID);
	}

	public DataModel getDataModel() {
		return delegate.getDataModel();
	}

	public List<RecommendedItem> recommend(long userID, int howMany)
			throws TasteException {
		return delegate.recommend(userID, howMany);
	}

	public List<RecommendedItem> recommend(long userID, int howMany, 
			IDRescorer rescorer)
					throws TasteException {
		return delegate.recommend(userID, howMany,rescorer);
	}

	public synchronized List<String> recommendLIkesForAnonymous(
			BooleanUserPreferenceArray preArray, int howMany)
					throws TasteException {
		plusModel.setTempPrefs(preArray);
		List<RecommendedItem> recommendations = recommend(
				PlusAnonymousUserDataModel.TEMP_USER_ID, howMany, null);
		plusModel.clearTempPrefs();
		if (recommendations != null) {
			ArrayList<String> ret = new ArrayList<String> ();
			for (RecommendedItem item: recommendations) {
				ret.add(thing2long.toStringID(item.getItemID()));
			}
			return ret;
		}
		return null;
	}

	public synchronized List<String> recommendLikesForAnonymous(String[] likes,
			int howMany) 
					throws TasteException {			
		BooleanUserPreferenceArray anonymousPrefs =
				new BooleanUserPreferenceArray(likes.length);

		for (int i=0; i<likes.length; i++) {
			anonymousPrefs.setItemID(i, thing2long.toLongID(likes[i]));
		}
		plusModel.setTempPrefs(anonymousPrefs);
		List<RecommendedItem> recommendations = recommend(
				PlusAnonymousUserDataModel.TEMP_USER_ID, howMany, null);
		plusModel.clearTempPrefs();

		if (recommendations != null) {
			ArrayList<String> ret = new ArrayList<String> ();
			for (RecommendedItem item: recommendations) {
				ret.add(thing2long.toStringID(item.getItemID()));
			}
			return ret;
		}
		return null;
	}

	public synchronized List<String> recommendUsersForAnonymous(
			String[] likes, int howMany)
					throws TasteException {
		BooleanUserPreferenceArray anonymousPrefs =
				new BooleanUserPreferenceArray(likes.length);

		for (int i=0; i<likes.length; i++) {
			anonymousPrefs.setItemID(i, thing2long.toLongID(likes[i]));
		}
		plusModel.setTempPrefs(anonymousPrefs);
		long [] similarUsers = this.mostSimilarUserIDs(
				PlusAnonymousUserDataModel.TEMP_USER_ID, howMany);
		if (similarUsers != null) {
			ArrayList<String> ret = new ArrayList<String> ();
			for (Long user: similarUsers) {
				StringBuilder sb = new StringBuilder();
				sb.append(thing2long.toStringID(user));
				sb.append(": <");
				for (long itemID: 
					plusModel.getPreferencesFromUser(user).getIDs()) {
					sb.append(thing2long.toStringID(itemID));
					sb.append(", ");
				}
				sb.replace(sb.length()-2, sb.length(), ">");
				sb.append("\n");
				ret.add(sb.toString());
			}
			return ret;
		}
		return null;
	}

	public List<String> recommendUsersForUsers(String[] users, int howMany)
			throws TasteException {
		PriorityQueue<SimilarUserPair> queue = 
				new PriorityQueue<SimilarUserPair>(howMany+1, new UserSimilarityComparator());
		for (String userStr: users) {
			long userID = thing2long.toLongID(userStr);
			String username = thing2long.toStringID(userID);
			if (username == null) {
				continue;
			}
			if (userStr.compareTo(username) == 0) {  // user exists
				// get all similar users of the current user.
				long [] similarUsers = this.mostSimilarUserIDs(
						userID, howMany);
				for (long user : similarUsers) {
					queue.add(new SimilarUserPair(userID, user, 
							this.delegate.getSimilarity().userSimilarity(
									user, userID)));
					if (queue.size() > howMany) {
						queue.poll();
					}
				}
			}
		}
		ArrayList<String> ret = new ArrayList<String> ();
		while(queue.size() != 0) {
			SimilarUserPair userPair = queue.remove();
			StringBuilder sb = new StringBuilder();
			sb.append(thing2long.toStringID(userPair.getUser2ID()));
			sb.append(": <");
			for (long itemID: 
				plusModel.getPreferencesFromUser(
						userPair.getUser2ID()).getIDs()) {
				sb.append(thing2long.toStringID(itemID));
				sb.append(", ");
			}
			sb.replace(sb.length()-2, sb.length(), ">\n");
			ret.add(0, sb.toString());
		}
		return ret;
	}

	public long[] mostSimilarUserIDs(long userID, int howMany) 
			throws TasteException {
		return delegate.mostSimilarUserIDs(userID, howMany);
	}

	public long[] mostSimilarUserIDs(long userID, int howMany,
			Rescorer<LongPair> rescorer) throws TasteException {
		return delegate.mostSimilarUserIDs(userID, howMany, rescorer);
	}

	public void removePreference(long userID, long itemID) 
			throws TasteException {
		delegate.removePreference(userID, itemID);

	}

	public void setPreference(long userID, long itemID, float value)
			throws TasteException {
		delegate.setPreference(userID, itemID, value);

	}
}