package com.ptms.mobile.utils;

import android.content.Context;
import android.util.Log;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.Volley;

import java.security.cert.X509Certificate;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * Gestionnaire API pour Volley (utilisé par les activités Chat et Notes)
 * Supporte les certificats SSL autosignés via SettingsManager
 */
public class ApiManager {
    private static final String TAG = "ApiManager";
    private static ApiManager instance;
    private RequestQueue requestQueue;
    private Context context;
    private SettingsManager settingsManager;

    private ApiManager(Context context) {
        this.context = context.getApplicationContext();
        this.settingsManager = new SettingsManager(context);
        this.requestQueue = getRequestQueue();
    }

    public static synchronized ApiManager getInstance(Context context) {
        if (instance == null) {
            instance = new ApiManager(context);
        }
        return instance;
    }

    public RequestQueue getRequestQueue() {
        if (requestQueue == null) {
            // Si ignore SSL est activé, créer une RequestQueue avec SSL trust
            if (settingsManager.isIgnoreSsl()) {
                Log.d(TAG, "SSL ignoré - Configuration de Volley pour certificats autosignés");
                requestQueue = createTrustAllRequestQueue();
            } else {
                Log.d(TAG, "SSL activé - Configuration de Volley standard");
                requestQueue = Volley.newRequestQueue(context);
            }
        }
        return requestQueue;
    }

    /**
     * Crée une RequestQueue qui accepte tous les certificats SSL
     */
    private RequestQueue createTrustAllRequestQueue() {
        try {
            // Créer un TrustManager qui accepte tous les certificats
            TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    @Override
                    public void checkClientTrusted(X509Certificate[] chain, String authType) {
                        // Accepter tous les certificats clients
                    }

                    @Override
                    public void checkServerTrusted(X509Certificate[] chain, String authType) {
                        // Accepter tous les certificats serveurs
                    }

                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }
                }
            };

            // Installer le TrustManager
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

            // Créer un HostnameVerifier qui accepte tous les hostnames
            HostnameVerifier allHostsValid = new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    return true; // Accepter tous les hostnames
                }
            };

            // Créer un HurlStack personnalisé avec notre SSL config
            HurlStack hurlStack = new HurlStack(null, sslSocketFactory) {
                @Override
                protected java.net.HttpURLConnection createConnection(java.net.URL url) throws java.io.IOException {
                    java.net.HttpURLConnection connection = super.createConnection(url);
                    if (connection instanceof HttpsURLConnection) {
                        ((HttpsURLConnection) connection).setSSLSocketFactory(sslSocketFactory);
                        ((HttpsURLConnection) connection).setHostnameVerifier(allHostsValid);
                    }
                    return connection;
                }
            };

            // Créer la RequestQueue avec notre HurlStack personnalisé
            return Volley.newRequestQueue(context, hurlStack);

        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de la configuration SSL pour Volley", e);
            // Fallback vers configuration standard
            return Volley.newRequestQueue(context);
        }
    }

    public <T> void addToRequestQueue(Request<T> req) {
        getRequestQueue().add(req);
    }

    public static String getBaseUrl() {
        if (instance != null && instance.settingsManager != null) {
            String url = instance.settingsManager.getServerUrl();
            // Retirer /api/ à la fin si présent pour compatibilité
            if (url.endsWith("/api/")) {
                return url.substring(0, url.length() - 4);
            }
            if (url.endsWith("/")) {
                return url.substring(0, url.length() - 1);
            }
            return url;
        }
        return "https://192.168.188.28";
    }

    /**
     * Rafraîchit la configuration (à appeler après modification des settings)
     */
    public void refreshConfiguration() {
        requestQueue = null; // Force la recréation à la prochaine utilisation
        Log.d(TAG, "Configuration rafraîchie");
    }
}
