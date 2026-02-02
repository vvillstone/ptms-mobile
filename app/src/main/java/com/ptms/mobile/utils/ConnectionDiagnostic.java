package com.ptms.mobile.utils;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * Utilitaire de diagnostic de connexion pour PTMS Mobile
 */
public class ConnectionDiagnostic {
    
    private static final String TAG = "ConnectionDiagnostic";
    
    /**
     * Interface pour recevoir les résultats du test
     */
    public interface DiagnosticCallback {
        void onSuccess(String message, String details);
        void onError(String message, String details);
    }
    
    /**
     * Teste la connexion à un serveur PTMS
     */
    public static void testConnection(String serverUrl, boolean ignoreSsl, DiagnosticCallback callback) {
        new Thread(() -> {
            try {
                Log.d(TAG, "Test de connexion vers: " + serverUrl);
                
                // Configurer SSL si nécessaire
                if (ignoreSsl && serverUrl.startsWith("https")) {
                    disableSslVerification();
                }
                
                // Test 1: Ping du serveur de base
                URL url = new URL(serverUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);
                
                int responseCode = connection.getResponseCode();
                String responseMessage = connection.getResponseMessage();
                
                Log.d(TAG, "Code de réponse: " + responseCode);
                Log.d(TAG, "Message: " + responseMessage);
                
                // Lire la réponse
                BufferedReader reader;
                if (responseCode >= 200 && responseCode < 300) {
                    reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                } else {
                    reader = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                }
                
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line).append("\n");
                }
                reader.close();
                
                String responseBody = response.toString();
                Log.d(TAG, "Réponse: " + responseBody.substring(0, Math.min(200, responseBody.length())));
                
                // Test 2: Vérifier l'endpoint de login
                String loginUrl = serverUrl + (serverUrl.endsWith("/") ? "" : "/") + "login.php";
                Log.d(TAG, "Test de l'endpoint login: " + loginUrl);
                
                URL loginUrlObj = new URL(loginUrl);
                HttpURLConnection loginConnection = (HttpURLConnection) loginUrlObj.openConnection();
                loginConnection.setRequestMethod("POST");
                loginConnection.setRequestProperty("Content-Type", "application/json");
                loginConnection.setConnectTimeout(10000);
                loginConnection.setReadTimeout(10000);
                
                int loginResponseCode = loginConnection.getResponseCode();
                Log.d(TAG, "Code de réponse login: " + loginResponseCode);
                
                // Construire le rapport de diagnostic
                StringBuilder diagnostic = new StringBuilder();
                diagnostic.append("URL testée: ").append(serverUrl).append("\n");
                diagnostic.append("Code de réponse: ").append(responseCode).append("\n");
                diagnostic.append("Message: ").append(responseMessage).append("\n");
                diagnostic.append("Login endpoint: ").append(loginResponseCode).append("\n");
                diagnostic.append("\nRéponse:\n").append(responseBody.substring(0, Math.min(500, responseBody.length())));
                
                if (responseCode >= 200 && responseCode < 500) {
                    callback.onSuccess("Connexion réussie!", diagnostic.toString());
                } else {
                    callback.onError("Erreur de connexion", diagnostic.toString());
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Erreur lors du test de connexion", e);
                String errorDetails = "Erreur: " + e.getClass().getSimpleName() + "\n" +
                                    "Message: " + e.getMessage() + "\n" +
                                    "URL testée: " + serverUrl;
                callback.onError("Échec de la connexion", errorDetails);
            }
        }).start();
    }
    
    /**
     * Teste plusieurs URL candidates pour trouver le bon serveur
     */
    public static void testMultipleUrls(Context context, DiagnosticCallback callback) {
        String[] candidateUrls = {
            "https://serveralpha.protti.group/api/",
            "https://192.168.188.28/api/",
            "http://192.168.188.28/api/",
            "https://192.168.188.21/api/",
            new SettingsManager(context).getServerUrl()
        };
        
        testUrlsSequentially(candidateUrls, 0, callback);
    }
    
    private static void testUrlsSequentially(String[] urls, int index, DiagnosticCallback callback) {
        if (index >= urls.length) {
            callback.onError("Aucun serveur accessible", "Tous les tests ont échoué");
            return;
        }
        
        testConnection(urls[index], true, new DiagnosticCallback() {
            @Override
            public void onSuccess(String message, String details) {
                callback.onSuccess("Serveur trouvé: " + urls[index], details);
            }
            
            @Override
            public void onError(String message, String details) {
                Log.d(TAG, "URL échouée: " + urls[index]);
                // Tester l'URL suivante
                testUrlsSequentially(urls, index + 1, callback);
            }
        });
    }
    
    /**
     * Désactive la vérification SSL (à utiliser uniquement en développement)
     */
    private static void disableSslVerification() {
        try {
            TrustManager[] trustAllCerts = new TrustManager[] {
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }
                    
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {
                    }
                    
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {
                    }
                }
            };
            
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de la désactivation SSL", e);
        }
    }
    
    /**
     * Teste l'authentification avec des credentials
     */
    public static void testLogin(String serverUrl, String email, String password, boolean ignoreSsl, DiagnosticCallback callback) {
        new Thread(() -> {
            try {
                if (ignoreSsl && serverUrl.startsWith("https")) {
                    disableSslVerification();
                }
                
                String loginUrl = serverUrl + (serverUrl.endsWith("/") ? "" : "/") + "login.php";
                URL url = new URL(loginUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setDoOutput(true);
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);
                
                // Préparer les données JSON
                String jsonInputString = "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
                
                // Envoyer les données
                try (java.io.OutputStream os = connection.getOutputStream()) {
                    byte[] input = jsonInputString.getBytes("utf-8");
                    os.write(input, 0, input.length);
                }
                
                // Lire la réponse
                int responseCode = connection.getResponseCode();
                BufferedReader reader;
                if (responseCode >= 200 && responseCode < 300) {
                    reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                } else {
                    reader = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                }
                
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                
                String responseBody = response.toString();
                Log.d(TAG, "Réponse login: " + responseBody);
                
                String diagnostic = "URL: " + loginUrl + "\n" +
                                  "Code: " + responseCode + "\n" +
                                  "Réponse: " + responseBody;
                
                if (responseCode == 200) {
                    callback.onSuccess("Login réussi!", diagnostic);
                } else {
                    callback.onError("Login échoué", diagnostic);
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Erreur lors du test de login", e);
                callback.onError("Erreur", e.getMessage());
            }
        }).start();
    }
}


