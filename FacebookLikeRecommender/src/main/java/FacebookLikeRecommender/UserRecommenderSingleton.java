package FacebookLikeRecommender;

import org.apache.mahout.common.ClassUtils;

public final class UserRecommenderSingleton {
	private final UserBasedAnonymousRecommender recommender;

	private static UserRecommenderSingleton instance;

	public static synchronized UserRecommenderSingleton getInstance() {
		if (instance == null) {
			throw new IllegalStateException("Not initialized");
		}
		return instance;
	}

	public static synchronized void initializeIfNeeded(String recommenderClassName) {
		if (instance == null) {
			instance = new UserRecommenderSingleton(recommenderClassName);
		}
	}

	private UserRecommenderSingleton(String recommenderClassName) {
		if (recommenderClassName == null) {
			throw new IllegalArgumentException("Recommender class name is null");
		}
		recommender = ClassUtils.instantiateAs(recommenderClassName, UserBasedAnonymousRecommender.class);
	}

	public UserBasedAnonymousRecommender getRecommender() {
		return recommender;
	}

}
