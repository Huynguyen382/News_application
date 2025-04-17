package com.example.vnews.Repository;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.vnews.Model.articles;
import com.example.vnews.Model.categories;
import com.example.vnews.Model.saved_articles;
import com.example.vnews.Model.users;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FirebaseRepository {
    private static final String TAG = "FirebaseRepository";
    
    // Firebase instances
    private final FirebaseFirestore db;
    private final FirebaseAuth mAuth;
    
    // Collection names
    private static final String ARTICLES_COLLECTION = "articles";
    private static final String USERS_COLLECTION = "users";
    private static final String SAVED_ARTICLES_COLLECTION = "saved_articles";
    private static final String CATEGORIES_COLLECTION = "categories";
    
    // Singleton instance
    private static volatile FirebaseRepository instance;
    
    // Private constructor for singleton pattern
    public FirebaseRepository() {
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
    }


    
    // Get singleton instance
    public static FirebaseRepository getInstance() {
        if (instance == null) {
            synchronized (FirebaseRepository.class) {
                if (instance == null) {
                    instance = new FirebaseRepository();
                }
            }
        }
        return instance;
    }
    
    // ===== ARTICLES METHODS =====
    
    /**
     * Get all articles
     */
    public void getAllArticles(final FirestoreCallback<List<articles>> callback) {
        db.collection(ARTICLES_COLLECTION)
                .orderBy("publishedAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<articles> articlesList = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        articles article = document.toObject(articles.class);
                        articlesList.add(article);
                    }
                    callback.onCallback(articlesList);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting articles", e);
                    callback.onError(e);
                });
    }
    
    /**
     * Get articles by category
     */
    public void getArticlesByCategory(String categoryId, final FirestoreCallback<List<articles>> callback) {
        db.collection(ARTICLES_COLLECTION)
                .whereEqualTo("categoryId", categoryId)
                .orderBy("publishedAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<articles> articlesList = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        articles article = document.toObject(articles.class);
                        articlesList.add(article);
                    }
                    callback.onCallback(articlesList);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting articles by category", e);
                    callback.onError(e);
                });
    }
    
    /**
     * Get article by id
     */
    public void getArticleById(String articleId, final FirestoreCallback<articles> callback) {
        db.collection(ARTICLES_COLLECTION)
                .document(articleId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        articles article = documentSnapshot.toObject(articles.class);
                        callback.onCallback(article);
                    } else {
                        callback.onCallback(null);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting article by id", e);
                    callback.onError(e);
                });
    }
    
    /**
     * Add article
     */
    public void addArticle(articles article, final FirestoreCallback<String> callback) {
        if (article.getId() == null || article.getId().isEmpty()) {
            DocumentReference docRef = db.collection(ARTICLES_COLLECTION).document();
            article.setId(docRef.getId());
        }
        
        db.collection(ARTICLES_COLLECTION)
                .document(article.getId())
                .set(article)
                .addOnSuccessListener(aVoid -> callback.onCallback(article.getId()))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error adding article", e);
                    callback.onError(e);
                });
    }
    
    /**
     * Update article
     */
    public void updateArticle(articles article, final FirestoreCallback<Void> callback) {
        db.collection(ARTICLES_COLLECTION)
                .document(article.getId())
                .set(article)
                .addOnSuccessListener(aVoid -> callback.onCallback(null))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating article", e);
                    callback.onError(e);
                });
    }
    
    /**
     * Delete article
     */
    public void deleteArticle(String articleId, final FirestoreCallback<Void> callback) {
        db.collection(ARTICLES_COLLECTION)
                .document(articleId)
                .delete()
                .addOnSuccessListener(aVoid -> callback.onCallback(null))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error deleting article", e);
                    callback.onError(e);
                });
    }
    
    // ===== USERS METHODS =====
    
    /**
     * Register user with email and password
     */
    public void registerUser(String email, String password, final FirestoreCallback<String> callback) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null && task.getResult().getUser() != null) {
                        String userId = task.getResult().getUser().getUid();
                        callback.onCallback(userId);
                    } else {
                        callback.onError(task.getException());
                    }
                });
    }
    
    /**
     * Login user with email and password
     */
    public void loginUser(String email, String password, final FirestoreCallback<String> callback) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null && task.getResult().getUser() != null) {
                        String userId = task.getResult().getUser().getUid();
                        callback.onCallback(userId);
                    } else {
                        callback.onError(task.getException());
                    }
                });
    }
    
    /**
     * Add user profile
     */
    public void addUserProfile(users user, final FirestoreCallback<Void> callback) {
        db.collection(USERS_COLLECTION)
                .document(user.getId())
                .set(user)
                .addOnSuccessListener(aVoid -> callback.onCallback(null))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error adding user profile", e);
                    callback.onError(e);
                });
    }
    
    /**
     * Get user profile
     */
    public void getUserProfile(String userId, final FirestoreCallback<users> callback) {
        db.collection(USERS_COLLECTION)
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        users user = documentSnapshot.toObject(users.class);
                        callback.onCallback(user);
                    } else {
                        callback.onCallback(null);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting user profile", e);
                    callback.onError(e);
                });
    }
    
    /**
     * Update user profile
     */
    public void updateUserProfile(users user, final FirestoreCallback<Void> callback) {
        db.collection(USERS_COLLECTION)
                .document(user.getId())
                .set(user)
                .addOnSuccessListener(aVoid -> callback.onCallback(null))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating user profile", e);
                    callback.onError(e);
                });
    }
    
    // ===== SAVED ARTICLES METHODS =====
    
    /**
     * Save article
     */
    public void saveArticle(String userId, String articleId, final FirestoreCallback<String> callback) {
        DocumentReference docRef = db.collection(SAVED_ARTICLES_COLLECTION).document();
        String id = docRef.getId();
        
        saved_articles savedArticle = new saved_articles(id, articleId, userId, System.currentTimeMillis());
        
        docRef.set(savedArticle)
                .addOnSuccessListener(aVoid -> callback.onCallback(id))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error saving article", e);
                    callback.onError(e);
                });
    }
    
    /**
     * Check if article is saved
     */
    public void isArticleSaved(String userId, String articleId, final FirestoreCallback<Boolean> callback) {
        db.collection(SAVED_ARTICLES_COLLECTION)
                .whereEqualTo("userId", userId)
                .whereEqualTo("articleId", articleId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    boolean isSaved = !queryDocumentSnapshots.isEmpty();
                    callback.onCallback(isSaved);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking if article is saved", e);
                    callback.onError(e);
                });
    }
    
    /**
     * Get saved articles for user
     */
    public void getSavedArticles(String userId, final FirestoreCallback<List<articles>> callback) {
        db.collection(SAVED_ARTICLES_COLLECTION)
                .whereEqualTo("userId", userId)
                .orderBy("savedAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<String> articleIds = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        saved_articles savedArticle = document.toObject(saved_articles.class);
                        articleIds.add(savedArticle.getArticleId());
                    }
                    
                    if (articleIds.isEmpty()) {
                        callback.onCallback(new ArrayList<>());
                        return;
                    }
                    
                    // Get the articles using the ids
                    getArticlesByIds(articleIds, callback);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting saved articles", e);
                    callback.onError(e);
                });
    }
    
    /**
     * Get articles by list of ids
     */
    private void getArticlesByIds(List<String> articleIds, final FirestoreCallback<List<articles>> callback) {
        List<articles> articlesList = new ArrayList<>();
        final int[] count = {0};
        
        for (String articleId : articleIds) {
            db.collection(ARTICLES_COLLECTION)
                    .document(articleId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            articles article = documentSnapshot.toObject(articles.class);
                            articlesList.add(article);
                        }
                        
                        count[0]++;
                        if (count[0] == articleIds.size()) {
                            callback.onCallback(articlesList);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error getting article by id", e);
                        count[0]++;
                        if (count[0] == articleIds.size()) {
                            callback.onCallback(articlesList);
                        }
                    });
        }
    }
    
    /**
     * Unsave article
     */
    public void unsaveArticle(String userId, String articleId, final FirestoreCallback<Void> callback) {
        db.collection(SAVED_ARTICLES_COLLECTION)
                .whereEqualTo("userId", userId)
                .whereEqualTo("articleId", articleId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        String savedArticleId = queryDocumentSnapshots.getDocuments().get(0).getId();
                        db.collection(SAVED_ARTICLES_COLLECTION)
                                .document(savedArticleId)
                                .delete()
                                .addOnSuccessListener(aVoid -> callback.onCallback(null))
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Error unsaving article", e);
                                    callback.onError(e);
                                });
                    } else {
                        callback.onCallback(null);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error finding saved article", e);
                    callback.onError(e);
                });
    }
    
    // ===== CATEGORIES METHODS =====
    
    /**
     * Get all categories
     */
    public void getAllCategories(final FirestoreCallback<List<categories>> callback) {
        db.collection(CATEGORIES_COLLECTION)
                .orderBy("name")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<categories> categoriesList = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        categories category = document.toObject(categories.class);
                        categoriesList.add(category);
                    }
                    callback.onCallback(categoriesList);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting categories", e);
                    callback.onError(e);
                });
    }
    
    /**
     * Add category
     */
    public void addCategory(categories category, final FirestoreCallback<String> callback) {
        if (category.getId() == null || category.getId().isEmpty()) {
            DocumentReference docRef = db.collection(CATEGORIES_COLLECTION).document();
            category.setId(docRef.getId());
        }
        
        db.collection(CATEGORIES_COLLECTION)
                .document(category.getId())
                .set(category)
                .addOnSuccessListener(aVoid -> callback.onCallback(category.getId()))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error adding category", e);
                    callback.onError(e);
                });
    }
    
    /**
     * Check if a user is currently logged in
     */
    public boolean isUserLoggedIn() {
        return mAuth.getCurrentUser() != null;
    }
    
    /**
     * Get current user ID
     */
    public String getCurrentUserId() {
        if (mAuth.getCurrentUser() != null) {
            return mAuth.getCurrentUser().getUid();
        }
        return null;
    }
    
    /**
     * Get current user name or email as fallback
     */
    public String getCurrentUserName() {
        if (mAuth.getCurrentUser() != null) {
            if (mAuth.getCurrentUser().getDisplayName() != null && 
                !mAuth.getCurrentUser().getDisplayName().isEmpty()) {
                return mAuth.getCurrentUser().getDisplayName();
            } else {
                return mAuth.getCurrentUser().getEmail();
            }
        }
        return "Khách";
    }
    
    /**
     * Callback interface for Firestore operations
     */
    public interface FirestoreCallback<T> {
        void onCallback(T result);
        void onError(Exception e);
    }
}
