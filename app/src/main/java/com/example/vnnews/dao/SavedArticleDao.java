package com.example.vnnews.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import com.example.vnnews.model.SavedArticle;
import java.util.List;

@Dao
public interface SavedArticleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(SavedArticle savedArticle);

    @Delete
    void delete(SavedArticle savedArticle);

    @Query("SELECT * FROM saved_articles WHERE userId = :userId")
    List<SavedArticle> getSavedArticlesByUserId(int userId);

    @Query("SELECT * FROM saved_articles WHERE userId = :userId AND articleId = :articleId")
    SavedArticle getSavedArticle(int userId, int articleId);
} 