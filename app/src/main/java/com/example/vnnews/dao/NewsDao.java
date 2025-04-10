package com.example.vnnews.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import com.example.vnnews.model.News;

import java.util.List;

@Dao
public interface NewsDao {
    @Insert
    void insert(News news);

    @Query("SELECT * FROM news")
    List<News> getAllNews();

    @Query("DELETE FROM news")
    void deleteAllNews();

    @Query("SELECT * FROM news WHERE id = :newsId")
    News getNewsById(long newsId);
}