package com.example.vnnews.database;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import com.example.vnnews.dao.UserDao;
import com.example.vnnews.dao.ArticleDao;
import com.example.vnnews.dao.CategoryDao;
import com.example.vnnews.dao.SavedArticleDao;
import com.example.vnnews.dao.NewsDao;
import com.example.vnnews.model.User;
import com.example.vnnews.model.Article;
import com.example.vnnews.model.Category;
import com.example.vnnews.model.SavedArticle;
import com.example.vnnews.model.News;

@Database(entities = {User.class, Article.class, Category.class, SavedArticle.class, News.class}, version = 2, exportSchema = false)
@TypeConverters({Converters.class})
public abstract class AppDatabase extends RoomDatabase {
    private static final String DATABASE_NAME = "vnnews_db";
    private static AppDatabase instance;

    public abstract UserDao userDao();
    public abstract ArticleDao articleDao();
    public abstract CategoryDao categoryDao();
    public abstract SavedArticleDao savedArticleDao();
    public abstract NewsDao newsDao();

    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(
                    context.getApplicationContext(),
                    AppDatabase.class,
                    DATABASE_NAME)
                    .fallbackToDestructiveMigration()
                    .build();
        }
        return instance;
    }
} 