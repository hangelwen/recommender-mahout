package FacebookLikeRecommender;

import java.util.List;

import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.impl.model.BooleanUserPreferenceArray;
import org.apache.mahout.cf.taste.recommender.UserBasedRecommender;

/**
 * This interface represents a user-based recommender that handles anonymous users.
 * @author leijikai
 *
 */
public interface UserBasedAnonymousRecommender extends UserBasedRecommender{
	//recommend for an anonymous user that is not in the data.
	public List<String> recommendLIkesForAnonymous(
			BooleanUserPreferenceArray preArray, int howMany)
			throws TasteException;
	public List<String> recommendLikesForAnonymous(
			String[] likes, int howMany)
			throws TasteException;
	public List<String> recommendUsersForAnonymous(String[] likes, int howMany)
			throws TasteException;
	public List<String> recommendUsersForUsers(String[] Users, int howMany)
			throws TasteException;
}
