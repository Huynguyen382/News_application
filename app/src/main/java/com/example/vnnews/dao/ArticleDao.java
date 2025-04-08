package com.example.myapplication.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.myapplication.model.Article;

import java.util.List;

@Dao
public interface ArticleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Article article);

    @Update
    void update(Article article);

    @Delete
    void delete(Article article);

    @Query("SELECT * FROM articles")
    LiveData<List<Article>> getAllArticles();

    @Query("SELECT * FROM articles WHERE category = :category")
    LiveData<List<Article>> getArticlesByCategory(String category);

    @Query("SELECT * FROM articles WHERE id = :id")
    LiveData<Article> getArticleById(int id);

    @Query("DELETE FROM articles")
    void deleteAll();
} 