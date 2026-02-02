package com.ptms.mobile.api;

import android.content.Context;

import com.ptms.mobile.utils.SettingsManager;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Client API configurable utilisant l'API unifiée PTMS
 */
public class ApiClient {

    private static ApiClient instance;
    private Retrofit retrofit;
    private ApiService apiService;
    private SettingsManager settingsManager;
    private CacheManager cacheManager;
    private Context context;
    
    private ApiClient(Context context) {
        this.context = context.getApplicationContext();
        settingsManager = new SettingsManager(context);
        cacheManager = new CacheManager(context);
        setupRetrofit();
    }
    
    public static synchronized ApiClient getInstance(Context context) {
        if (instance == null) {
            instance = new ApiClient(context.getApplicationContext());
        }
        return instance;
    }
    
    private void setupRetrofit() {
        // Configuration du client HTTP
        OkHttpClient.Builder httpClient = new OkHttpClient.Builder()
                .connectTimeout(settingsManager.getTimeout(), TimeUnit.SECONDS)
                .readTimeout(settingsManager.getTimeout(), TimeUnit.SECONDS)
                .writeTimeout(settingsManager.getTimeout(), TimeUnit.SECONDS);

        // ✅ Configuration du cache HTTP
        if (cacheManager != null && cacheManager.getOkHttpCache() != null) {
            httpClient.cache(cacheManager.getOkHttpCache());
            android.util.Log.d("API_CLIENT", "✅ Cache HTTP activé (" +
                    cacheManager.formatSize(cacheManager.getCacheMaxSize()) + ")");
        }

        // ✅ Intercepteur de cache intelligent
        httpClient.addInterceptor(new CacheInterceptor());

        // ✅ SÉCURITÉ: Logging conditionnel basé sur le mode debug
        if (settingsManager.isDebugMode()) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);
            httpClient.addInterceptor(logging);
            android.util.Log.d("API_CLIENT", "✅ Mode debug activé - Logging HTTP BODY");
        } else {
            // En production, logging minimal (seulement les erreurs)
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.NONE);
            android.util.Log.d("API_CLIENT", "✅ Mode production - Logging HTTP désactivé");
        }
        
        // Interceptor pour ajouter automatiquement "Bearer " au token
        httpClient.addInterceptor(chain -> {
            okhttp3.Request originalRequest = chain.request();
            okhttp3.Request.Builder requestBuilder = originalRequest.newBuilder();

            // Vérifier si l'Authorization header existe et ne commence pas par "Bearer "
            String authHeader = originalRequest.header("Authorization");
            if (authHeader != null && !authHeader.startsWith("Bearer ")) {
                requestBuilder.removeHeader("Authorization");
                requestBuilder.addHeader("Authorization", "Bearer " + authHeader);
                android.util.Log.d("API_CLIENT", "Token formaté: Bearer " + authHeader.substring(0, Math.min(20, authHeader.length())) + "...");
            }

            okhttp3.Request request = requestBuilder.build();
            android.util.Log.d("API_CLIENT", "Envoi requête vers: " + request.url());
            okhttp3.Response response = chain.proceed(request);
            android.util.Log.d("API_CLIENT", "Réponse reçue: " + response.code() + " " + response.message());
            return response;
        });
        
        // Configuration SSL si nécessaire
        if (settingsManager.isIgnoreSsl()) {
            try {
                // Créer un TrustManager qui accepte tous les certificats
                javax.net.ssl.TrustManager[] trustAllCerts = new javax.net.ssl.TrustManager[] {
                    new javax.net.ssl.X509TrustManager() {
                        @Override
                        public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) {
                        }

                        @Override
                        public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) {
                        }

                        @Override
                        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                            return new java.security.cert.X509Certificate[]{};
                        }
                    }
                };

                // Installer le TrustManager
                javax.net.ssl.SSLContext sslContext = javax.net.ssl.SSLContext.getInstance("SSL");
                sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
                
                // Créer un SSLSocketFactory qui utilise notre TrustManager
                javax.net.ssl.SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
                
                // Configurer le client pour ignorer SSL
                httpClient.sslSocketFactory(sslSocketFactory, (javax.net.ssl.X509TrustManager) trustAllCerts[0]);
                httpClient.hostnameVerifier(new javax.net.ssl.HostnameVerifier() {
                    @Override
                    public boolean verify(String hostname, javax.net.ssl.SSLSession session) {
                        return true; // Accepter tous les hostnames
                    }
                });
                
                android.util.Log.d("API_CLIENT", "SSL ignoré - Connexions non sécurisées activées");
            } catch (Exception e) {
                android.util.Log.e("API_CLIENT", "Erreur configuration SSL", e);
            }
        } else {
            android.util.Log.d("API_CLIENT", "SSL activé - Connexions sécurisées");
        }
        
        // Configuration de Retrofit avec Gson non strict
        com.google.gson.Gson gson = new com.google.gson.GsonBuilder()
                .setLenient()
                .setDateFormat("yyyy-MM-dd HH:mm:ss") // Format MySQL/PHP
                .create();
                
        // Utiliser l'URL de base configurée (fallback vers anciens endpoints)
        String baseUrl = settingsManager.getServerUrl();
        // Ne plus forcer l'API unifiée, utiliser l'URL telle quelle
        
        retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(httpClient.build())
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();
        
        apiService = retrofit.create(ApiService.class);
    }
    
    public ApiService getApiService() {
        return apiService;
    }
    
    public void refreshConfiguration() {
        setupRetrofit();
    }
    
    public void updateBaseUrl(String newBaseUrl) {
        // Méthode pour mettre à jour l'URL de base dynamiquement
        if (newBaseUrl != null && !newBaseUrl.isEmpty()) {
            settingsManager.setServerUrl(newBaseUrl);
            setupRetrofit();
        }
    }
    
    public String getBaseUrl() {
        return settingsManager.getServerUrl();
    }
    
    public String getUnifiedApiUrl() {
        // Retourner l'URL de base configurée (maintenant les anciens endpoints)
        return getBaseUrl();
    }

    /**
     * ✅ Obtient le gestionnaire de cache
     */
    public CacheManager getCacheManager() {
        return cacheManager;
    }

    /**
     * ✅ Affiche les statistiques du cache
     */
    public void logCacheStatistics() {
        if (cacheManager != null) {
            cacheManager.logCacheStatistics();
        }
    }

    /**
     * ✅ Nettoie le cache (supprime entrées expirées)
     */
    public void cleanCache() {
        if (cacheManager != null) {
            cacheManager.cleanCache();
        }
    }

    /**
     * ✅ Vide complètement le cache
     */
    public void clearCache() {
        if (cacheManager != null) {
            cacheManager.clearCache();
        }
    }
}
