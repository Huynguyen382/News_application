package com.example.vnnews.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import com.example.vnnews.model.Article;
import java.util.List;

@Dao
public interface ArticleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Article article);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<Article> articles);

    @Update
    void update(Article article);

    @Delete
    void delete(Article article);

    @Query("DELETE FROM articles")
    void deleteAll();

    @Query("SELECT * FROM articles")
    LiveData<List<Article>> getAllArticles();

    @Query("SELECT * FROM articles WHERE categoryId = :categoryId")
    LiveData<List<Article>> getArticlesByCategory(int categoryId);

    @Query("SELECT * FROM articles WHERE id = :id")
    LiveData<Article> getArticleById(int id);

    @Query("SELECT * FROM articles ORDER BY publishedAt DESC LIMIT :limit")
    LiveData<List<Article>> getLatestArticles(int limit);
} 