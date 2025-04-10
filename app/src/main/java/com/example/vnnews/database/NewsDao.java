package com.example.vnnews.database;

import androidx.room.Dao;
import androidx.room.Query;
import com.example.vnnews.model.News;

@Dao
public interface NewsDao {
    @Query("SELECT * FROM news WHERE id = :newsId")
    News getNewsById(long newsId);
} 