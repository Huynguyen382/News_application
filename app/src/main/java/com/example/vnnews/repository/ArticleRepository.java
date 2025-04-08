package com.example.myapplication.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.example.myapplication.dao.ArticleDao;
import com.example.myapplication.database.AppDatabase;
import com.example.myapplication.model.Article;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ArticleRepository {
    private ArticleDao articleDao;
    private LiveData<List<Article>> allArticles;
    private ExecutorService executorService;

    public ArticleRepository(Application application) {
        AppDatabase database = AppDatabase.getInstance(application);
        articleDao = database.articleDao();
        allArticles = articleDao.getAllArticles();
        executorService = Executors.newSingleThreadExecutor();
    }

    public LiveData<List<Article>> getAllArticles() {
        return allArticles;
    }

    public LiveData<List<Article>> getArticlesByCategory(String category) {
        return articleDao.getArticlesByCategory(category);
    }

    public LiveData<Article> getArticleById(int id) {
        return articleDao.getArticleById(id);
    }

    public void insert(Article article) {
        executorService.execute(() -> articleDao.insert(article));
    }

    public void update(Article article) {
        executorService.execute(() -> articleDao.update(article));
    }

    public void delete(Article article) {
        executorService.execute(() -> articleDao.delete(article));
    }

    public void deleteAll() {
        executorService.execute(articleDao::deleteAll);
    }
} 