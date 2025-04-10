package com.example.vnnews.viewmodel;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.vnnews.model.Article;
import com.example.vnnews.repository.ArticleRepository;

import java.util.List;

public class ArticleViewModel extends AndroidViewModel {
    private ArticleRepository repository;
    private LiveData<List<Article>> allArticles;

    public ArticleViewModel(Application application) {
        super(application);
        repository = new ArticleRepository(application);
        allArticles = repository.getAllArticles();
    }

    public LiveData<List<Article>> getAllArticles() {
        return allArticles;
    }

    public LiveData<List<Article>> getArticlesByCategory(int categoryId) {
        return repository.getArticlesByCategory(categoryId);
    }

    public LiveData<Article> getArticleById(int id) {
        return repository.getArticleById(id);
    }

    public void insert(Article article) {
        repository.insert(article);
    }

    public void update(Article article) {
        repository.update(article);
    }

    public void delete(Article article) {
        repository.delete(article);
    }

    public void deleteAll() {
        repository.deleteAll();
    }
} 