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
import java.util.Locale;

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
     * Login user with username
     */
    public void loginWithUsername(String username, String password, final FirestoreCallback<String> callback) {
        Log.d(TAG, "Bắt đầu đăng nhập với tên đăng nhập: " + username);
        
        // Đầu tiên tìm kiếm user theo username
        db.collection(USERS_COLLECTION)
                .whereEqualTo("username", username)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Log.d(TAG, "Kết quả tìm kiếm: " + queryDocumentSnapshots.size() + " documents");
                    
                    if (!queryDocumentSnapshots.isEmpty()) {
                        // Tìm thấy user, lấy email để đăng nhập
                        DocumentSnapshot document = queryDocumentSnapshots.getDocuments().get(0);
                        users user = document.toObject(users.class);
                        Log.d(TAG, "Đã tìm thấy user: " + document.getId());
                        
                        if (user != null) {
                            Log.d(TAG, "Thông tin user: username=" + user.getUsername() + ", email=" + user.getEmail());
                            
                            if (user.getEmail() != null && !user.getEmail().isEmpty()) {
                                // Đăng nhập với email và password
                                Log.d(TAG, "Tiếp tục đăng nhập với email: " + user.getEmail());
                                loginUser(user.getEmail(), password, callback);
                            } else {
                                Log.e(TAG, "Email người dùng rỗng hoặc null");
                                callback.onError(new Exception("Không tìm thấy thông tin email"));
                            }
                        } else {
                            Log.e(TAG, "Không thể chuyển đổi document thành đối tượng users");
                            callback.onError(new Exception("Không tìm thấy thông tin tài khoản"));
                        }
                    } else {
                        Log.e(TAG, "Không tìm thấy tên đăng nhập: " + username);
                        
                        // Thử tìm kiếm không phân biệt hoa thường (cách này chỉ để debug)
                        db.collection(USERS_COLLECTION)
                                .get()
                                .addOnSuccessListener(allUsers -> {
                                    boolean found = false;
                                    for (DocumentSnapshot doc : allUsers.getDocuments()) {
                                        users u = doc.toObject(users.class);
                                        if (u != null && u.getUsername() != null) {
                                            Log.d(TAG, "User trong DB: " + u.getUsername());
                                            if (u.getUsername().equalsIgnoreCase(username)) {
                                                Log.d(TAG, "Tìm thấy user khi so sánh không phân biệt hoa thường: " + u.getUsername());
                                                found = true;
                                            }
                                        }
                                    }
                                    
                                    if (!found) {
                                        Log.e(TAG, "Không tìm thấy user nào có username tương tự: " + username);
                                    }
                                    
                                    callback.onError(new Exception("Tên đăng nhập không tồn tại"));
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Lỗi khi tìm kiếm tất cả người dùng", e);
                                    callback.onError(new Exception("Tên đăng nhập không tồn tại"));
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Lỗi khi tìm kiếm người dùng theo username", e);
                    callback.onError(e);
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
     * Check if username exists
     */
    public void isUsernameExists(String username, final FirestoreCallback<Boolean> callback) {
        Log.d(TAG, "Kiểm tra username tồn tại: " + username);
        
        db.collection(USERS_COLLECTION)
                .whereEqualTo("username", username)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    boolean exists = !queryDocumentSnapshots.isEmpty();
                    Log.d(TAG, "Username " + username + " " + (exists ? "đã tồn tại" : "chưa tồn tại"));
                    
                    if (exists) {
                        // Log các tài khoản được tìm thấy (để debug)
                        for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                            users user = doc.toObject(users.class);
                            if (user != null) {
                                Log.d(TAG, "User được tìm thấy: " + user.getUsername() + ", email: " + user.getEmail());
                            }
                        }
                    }
                    
                    callback.onCallback(exists);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Lỗi khi kiểm tra username", e);
                    callback.onError(e);
                });
    }
    
    /**
     * Get user profile
     */
    public void getUserProfile(String userId, final FirestoreCallback<users> callback) {
        Log.d(TAG, "Getting user profile for userId: " + userId);
        
        // First try SERVER_WITH_CACHE_FALLBACK
        db.collection(USERS_COLLECTION)
                .document(userId)
                .get(com.google.firebase.firestore.Source.SERVER)
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        users user = documentSnapshot.toObject(users.class);
                        Log.d(TAG, "Successfully retrieved user profile from Firestore server");
                        callback.onCallback(user);
                    } else {
                        Log.d(TAG, "User document does not exist on server");
                        callback.onCallback(null);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting user profile from server", e);
                    // Try getting from cache if there's a network error
                    tryGetUserProfileFromCache(userId, callback, e);
                });
    }
    
    /**
     * Try to get user profile from cache if online fetch failed
     */
    private void tryGetUserProfileFromCache(String userId, final FirestoreCallback<users> callback, Exception originalException) {
        Log.d(TAG, "Attempting to get user profile from cache");
        
        // Try to get from default source (which will use cache if available)
        db.collection(USERS_COLLECTION)
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        users user = documentSnapshot.toObject(users.class);
                        Log.d(TAG, "Successfully retrieved user profile from default source");
                        callback.onCallback(user);
                    } else {
                        // Try one last approach - create a dummy user with just the ID
                        Log.d(TAG, "User not found in default source - creating offline placeholder");
                        tryCreateOfflinePlaceholder(userId, callback, originalException);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting user profile from default source", e);
                    tryCreateOfflinePlaceholder(userId, callback, originalException);
                });
    }
    
    /**
     * Create a placeholder user when offline
     */
    private void tryCreateOfflinePlaceholder(String userId, final FirestoreCallback<users> callback, Exception originalException) {
        // If we can't get the user from server or cache, create a basic placeholder
        // This allows the UI to show something rather than an error
        Log.d(TAG, "Creating offline placeholder user");
        
        try {
            // Create a basic user with just the ID
            users offlineUser = new users();
            offlineUser.setId(userId);
            offlineUser.setUsername("offline_user");
            
            // Get email from Firebase Auth if available, otherwise use placeholder
            String email = "unavailable@offline.mode";
            if (mAuth.getCurrentUser() != null && mAuth.getCurrentUser().getEmail() != null) {
                email = mAuth.getCurrentUser().getEmail();
                Log.d(TAG, "Using email from Firebase Auth for offline placeholder: " + email);
            }
            offlineUser.setEmail(email);
            
            callback.onCallback(offlineUser);
        } catch (Exception e) {
            Log.e(TAG, "Failed to create offline placeholder", e);
            callback.onError(originalException);
        }
    }
    
    /**
     * Get all users (mostly for debugging)
     */
    public void getAllUsers(final FirestoreCallback<List<users>> callback) {
        db.collection(USERS_COLLECTION)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<users> usersList = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        users user = document.toObject(users.class);
                        usersList.add(user);
                    }
                    callback.onCallback(usersList);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting all users", e);
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
        if (userId == null || articleId == null) {
            callback.onError(new Exception("User ID hoặc Article ID không được để trống"));
            return;
        }
        
        try {
            DocumentReference docRef = db.collection(SAVED_ARTICLES_COLLECTION).document();
            String id = docRef.getId();
            
            saved_articles savedArticle = new saved_articles(id, articleId, userId, System.currentTimeMillis());
            
            docRef.set(savedArticle)
                    .addOnSuccessListener(aVoid -> callback.onCallback(id))
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error saving article", e);
                        callback.onError(e);
                    });
        } catch (Exception e) {
            Log.e(TAG, "Error in saveArticle", e);
            callback.onError(e);
        }
    }
    
    /**
     * Check if article is saved
     */
    public void isArticleSaved(String userId, String articleId, final FirestoreCallback<Boolean> callback) {
        if (userId == null || articleId == null) {
            callback.onCallback(false);
            return;
        }
        
        try {
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
                        callback.onCallback(false);
                    });
        } catch (Exception e) {
            Log.e(TAG, "Error in isArticleSaved", e);
            callback.onCallback(false);
        }
    }
    
    /**
     * Get saved articles for user
     */
    public void getSavedArticles(String userId, final FirestoreCallback<List<articles>> callback) {
        if (userId == null) {
            callback.onCallback(new ArrayList<>());
            return;
        }
        
        try {
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
                        callback.onError(new Exception("Không thể tải bài viết đã lưu. Lỗi: " + e.getMessage()));
                    });
        } catch (Exception e) {
            Log.e(TAG, "Error in getSavedArticles", e);
            callback.onCallback(new ArrayList<>());
        }
    }
    
    /**
     * Get articles by list of ids
     */
    private void getArticlesByIds(List<String> articleIds, final FirestoreCallback<List<articles>> callback) {
        if (articleIds == null || articleIds.isEmpty()) {
            callback.onCallback(new ArrayList<>());
            return;
        }
        
        List<articles> articlesList = new ArrayList<>();
        final int[] count = {0};
        
        try {
            for (String articleId : articleIds) {
                // Dùng URL để tạo bài viết giả
                if (articleId.startsWith("http")) {
                    articles article = createArticleFromUrl(articleId);
                    articlesList.add(article);
                    count[0]++;
                    
                    if (count[0] == articleIds.size()) {
                        callback.onCallback(articlesList);
                    }
                    continue;
                }
                
                db.collection(ARTICLES_COLLECTION)
                        .document(articleId)
                        .get()
                        .addOnSuccessListener(documentSnapshot -> {
                            if (documentSnapshot.exists()) {
                                articles article = documentSnapshot.toObject(articles.class);
                                articlesList.add(article);
                            } else {
                                // Nếu không tìm thấy, thử tạo bài viết giả từ URL
                                articles article = createArticleFromUrl(articleId);
                                if (article != null) {
                                    articlesList.add(article);
                                }
                            }
                            
                            count[0]++;
                            if (count[0] == articleIds.size()) {
                                callback.onCallback(articlesList);
                            }
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Error getting article by id", e);
                            
                            // Nếu lỗi, thử tạo bài viết giả từ URL
                            articles article = createArticleFromUrl(articleId);
                            if (article != null) {
                                articlesList.add(article);
                            }
                            
                            count[0]++;
                            if (count[0] == articleIds.size()) {
                                callback.onCallback(articlesList);
                            }
                        });
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in getArticlesByIds", e);
            callback.onCallback(articlesList);
        }
    }
    
    /**
     * Tạo bài viết giả từ URL
     */
    private articles createArticleFromUrl(String url) {
        if (url == null || url.isEmpty()) {
            return null;
        }
        
        articles article = new articles();
        article.setId(url);
        
        // Parse URL để lấy tiêu đề
        String title = "";
        String imageUrl = "";
        String domain = "";
        
        try {
            // Phân tích URL để lấy tên miền
            String[] urlParts = url.split("//");
            if (urlParts.length > 1) {
                String[] domainParts = urlParts[1].split("/");
                domain = domainParts[0];
            }
            
            // Lấy phần cuối của URL làm tiêu đề
            String[] segments = url.split("/");
            String lastSegment = segments[segments.length - 1];
            
            // Loại bỏ phần mở rộng file và tham số truy vấn nếu có
            if (lastSegment.contains(".")) {
                lastSegment = lastSegment.substring(0, lastSegment.lastIndexOf("."));
            }
            if (lastSegment.contains("?")) {
                lastSegment = lastSegment.substring(0, lastSegment.indexOf("?"));
            }
            
            // Thay thế dấu gạch ngang và dấu gạch dưới bằng khoảng trắng
            lastSegment = lastSegment.replace("-", " ").replace("_", " ");
            
            // Nếu tiêu đề chứa ký tự mã hóa URL, decode nó với UTF-8
            if (lastSegment.contains("%")) {
                try {
                    lastSegment = java.net.URLDecoder.decode(lastSegment, "UTF-8");
                } catch (Exception e) {
                    Log.e(TAG, "Error decoding URL segment", e);
                }
            }
            
            // Viết hoa chữ cái đầu mỗi từ và giới hạn độ dài
            String[] words = lastSegment.split(" ");
            StringBuilder titleBuilder = new StringBuilder();
            for (String word : words) {
                if (!word.isEmpty()) {
                    // Xử lý đặc biệt cho tiếng Việt
                    if (word.length() > 1) {
                        // Lấy ký tự đầu tiên để viết hoa
                        char firstChar = word.charAt(0);
                        String restOfWord = word.substring(1);
                        
                        // Xử lý cho UTF-8
                        String uppercaseChar = String.valueOf(firstChar).toUpperCase(Locale.forLanguageTag("vi-VN"));
                        String lowercaseRest = restOfWord.toLowerCase(Locale.forLanguageTag("vi-VN"));
                        
                        titleBuilder.append(uppercaseChar)
                                .append(lowercaseRest)
                                .append(" ");
                    } else {
                        titleBuilder.append(word.toUpperCase(Locale.forLanguageTag("vi-VN")))
                                .append(" ");
                    }
                }
            }
            title = titleBuilder.toString().trim();
            
            // Giới hạn độ dài tiêu đề
            if (title.length() > 100) {
                title = title.substring(0, 97) + "...";
            }
            
            // Chọn hình ảnh phù hợp dựa vào tên miền
            if (domain.contains("vnexpress")) {
                imageUrl = "https://s1.vnecdn.net/vnexpress/restruct/i/v630/v2_2019/pc/graphics/logo.svg";
            } else if (domain.contains("tuoitre")) {
                imageUrl = "https://dangkyxettuyennghe.tuoitre.vn/img/logo.png";
            } else if (domain.contains("thanhnien")) {
                imageUrl = "https://static.thanhnien.vn/v2/App_Themes/images/logo-2019.png";
            } else if (domain.contains("dantri")) {
                imageUrl = "https://cdnweb.dantri.com.vn/dist/static-avatar.1-0-1.3a93b5.png";
            } else {
                // Hình ảnh mặc định cho các trang web khác
                imageUrl = "https://cdn-icons-png.flaticon.com/512/21/21601.png";
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error parsing URL", e);
            title = "Bài viết từ " + (domain.isEmpty() ? url : domain);
        }
        
        if (title.isEmpty()) {
            title = "Bài viết đã lưu";
        }
        
        article.setTitle(title);
        article.setContent("Nội dung bài viết đã lưu từ " + (domain.isEmpty() ? url : domain));
        article.setPublishedAt(String.valueOf(System.currentTimeMillis()));
        article.setAuthor(domain);
        article.setImgUrl(imageUrl);
        
        return article;
    }
    
    /**
     * Unsave article
     */
    public void unsaveArticle(String userId, String articleId, final FirestoreCallback<Void> callback) {
        if (userId == null || articleId == null) {
            callback.onCallback(null);
            return;
        }
        
        try {
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
                                        callback.onCallback(null);
                                    });
                        } else {
                            callback.onCallback(null);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error finding saved article", e);
                        callback.onCallback(null);
                    });
        } catch (Exception e) {
            Log.e(TAG, "Error in unsaveArticle", e);
            callback.onCallback(null);
        }
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
     * Logout current user
     */
    public void logoutUser() {
        if (mAuth != null) {
            mAuth.signOut();
        }
    }
    
    /**
     * Callback interface for Firestore operations
     */
    public interface FirestoreCallback<T> {
        void onCallback(T result);
        void onError(Exception e);
    }
}
