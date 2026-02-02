package com.ptms.mobile.utils;

import android.content.Context;
import android.util.Log;

import com.ptms.mobile.api.ApiClient;
import com.ptms.mobile.api.ApiService;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Helper pour les nouvelles fonctionnalités de l'API unifiée
 */
public class UnifiedApiHelper {
    
    private static final String TAG = "UnifiedApiHelper";
    private ApiClient apiClient;
    private ApiService apiService;
    
    public UnifiedApiHelper(Context context) {
        apiClient = ApiClient.getInstance(context);
        apiService = apiClient.getApiService();
    }
    
    /**
     * Vérifier le statut de tous les services
     */
    public void checkSystemStatus(String token, SystemStatusCallback callback) {
        apiService.getSystemStatus("Bearer " + token).enqueue(new Callback<ApiService.SystemStatusResponse>() {
            @Override
            public void onResponse(Call<ApiService.SystemStatusResponse> call, Response<ApiService.SystemStatusResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiService.SystemStatusResponse status = response.body();
                    callback.onSuccess(status);
                } else {
                    callback.onError("Erreur lors de la récupération du statut: " + response.code());
                }
            }
            
            @Override
            public void onFailure(Call<ApiService.SystemStatusResponse> call, Throwable t) {
                Log.e(TAG, "Erreur lors de la vérification du statut", t);
                callback.onError("Erreur de connexion: " + t.getMessage());
            }
        });
    }
    
    /**
     * Recherche globale dans tous les services
     */
    public void globalSearch(String token, String query, String services, GlobalSearchCallback callback) {
        apiService.globalSearch("Bearer " + token, query, services).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Object> results = response.body();
                    callback.onSuccess(results);
                } else {
                    callback.onError("Erreur lors de la recherche: " + response.code());
                }
            }
            
            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Log.e(TAG, "Erreur lors de la recherche globale", t);
                callback.onError("Erreur de connexion: " + t.getMessage());
            }
        });
    }
    
    /**
     * Vérifier si l'API unifiée est disponible
     */
    public void checkApiAvailability(String token, ApiAvailabilityCallback callback) {
        checkSystemStatus(token, new SystemStatusCallback() {
            @Override
            public void onSuccess(ApiService.SystemStatusResponse status) {
                boolean isAvailable = status.success && status.services != null;
                callback.onAvailabilityChecked(isAvailable);
            }
            
            @Override
            public void onError(String error) {
                callback.onAvailabilityChecked(false);
            }
        });
    }
    
    /**
     * Obtenir l'URL de l'API unifiée
     */
    public String getUnifiedApiUrl() {
        return apiClient.getUnifiedApiUrl();
    }
    
    /**
     * Callback pour le statut des services
     */
    public interface SystemStatusCallback {
        void onSuccess(ApiService.SystemStatusResponse status);
        void onError(String error);
    }
    
    /**
     * Callback pour la recherche globale
     */
    public interface GlobalSearchCallback {
        void onSuccess(Map<String, Object> results);
        void onError(String error);
    }
    
    /**
     * Callback pour la disponibilité de l'API
     */
    public interface ApiAvailabilityCallback {
        void onAvailabilityChecked(boolean isAvailable);
    }
}
