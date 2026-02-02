package com.ptms.mobile.utils;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.util.Log;

import androidx.core.app.ActivityCompat;

/**
 * Utilitaires pour la gestion du réseau et des permissions
 */
public class NetworkUtils {
    
    private static final String TAG = "NetworkUtils";
    
    // ==================== VÉRIFICATION DE CONNECTIVITÉ ====================
    
    /**
     * Vérifier si l'appareil est connecté à internet
     */
    public static boolean isOnline(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        
        if (cm == null) {
            Log.w(TAG, "ConnectivityManager non disponible");
            return false;
        }
        
        // Vérifier les permissions pour Android 6.0+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_NETWORK_STATE) 
                != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "Permission ACCESS_NETWORK_STATE non accordée");
                return false;
            }
        }
        
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        boolean isConnected = networkInfo != null && networkInfo.isConnected();
        
        Log.d(TAG, "État de la connexion: " + (isConnected ? "Connecté" : "Hors ligne"));
        return isConnected;
    }
    
    /**
     * Vérifier si l'appareil est connecté via WiFi
     */
    public static boolean isConnectedViaWiFi(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        
        if (cm == null) {
            return false;
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_NETWORK_STATE) 
                != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        return networkInfo != null && 
               networkInfo.isConnected() && 
               networkInfo.getType() == ConnectivityManager.TYPE_WIFI;
    }
    
    /**
     * Vérifier si l'appareil est connecté via données mobiles
     */
    public static boolean isConnectedViaMobile(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        
        if (cm == null) {
            return false;
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_NETWORK_STATE) 
                != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        return networkInfo != null && 
               networkInfo.isConnected() && 
               networkInfo.getType() == ConnectivityManager.TYPE_MOBILE;
    }
    
    // ==================== TYPES DE CONNEXION ====================
    
    /**
     * Obtenir le type de connexion actuel
     */
    public static String getConnectionType(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        
        if (cm == null) {
            return "Inconnu";
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_NETWORK_STATE) 
                != PackageManager.PERMISSION_GRANTED) {
                return "Permission manquante";
            }
        }
        
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        
        if (networkInfo == null || !networkInfo.isConnected()) {
            return "Hors ligne";
        }
        
        switch (networkInfo.getType()) {
            case ConnectivityManager.TYPE_WIFI:
                return "WiFi";
            case ConnectivityManager.TYPE_MOBILE:
                return "Données mobiles";
            case ConnectivityManager.TYPE_ETHERNET:
                return "Ethernet";
            case ConnectivityManager.TYPE_BLUETOOTH:
                return "Bluetooth";
            default:
                return "Autre";
        }
    }
    
    /**
     * Obtenir la vitesse de la connexion (approximative)
     */
    public static String getConnectionSpeed(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        
        if (cm == null) {
            return "Inconnue";
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_NETWORK_STATE) 
                != PackageManager.PERMISSION_GRANTED) {
                return "Permission manquante";
            }
        }
        
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        
        if (networkInfo == null || !networkInfo.isConnected()) {
            return "Hors ligne";
        }
        
        switch (networkInfo.getType()) {
            case ConnectivityManager.TYPE_WIFI:
                return "Rapide (WiFi)";
            case ConnectivityManager.TYPE_MOBILE:
                // Essayer de déterminer le type de réseau mobile
                switch (networkInfo.getSubtype()) {
                    case 13: // LTE
                        return "Très rapide (4G LTE)";
                    case 15: // HSPA+
                        return "Rapide (3G+)";
                    case 3: // UMTS
                        return "Modéré (3G)";
                    case 1: // GPRS
                        return "Lent (2G)";
                    default:
                        return "Modéré (Mobile)";
                }
            case ConnectivityManager.TYPE_ETHERNET:
                return "Très rapide (Ethernet)";
            default:
                return "Inconnue";
        }
    }
    
    // ==================== QUALITÉ DE CONNEXION ====================
    
    /**
     * Évaluer la qualité de la connexion pour la synchronisation
     */
    public static ConnectionQuality getConnectionQuality(Context context) {
        if (!isOnline(context)) {
            return ConnectionQuality.OFFLINE;
        }
        
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        
        if (cm == null) {
            return ConnectionQuality.POOR;
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_NETWORK_STATE) 
                != PackageManager.PERMISSION_GRANTED) {
                return ConnectionQuality.POOR;
            }
        }
        
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        
        if (networkInfo == null || !networkInfo.isConnected()) {
            return ConnectionQuality.OFFLINE;
        }
        
        switch (networkInfo.getType()) {
            case ConnectivityManager.TYPE_WIFI:
                return ConnectionQuality.EXCELLENT;
            case ConnectivityManager.TYPE_ETHERNET:
                return ConnectionQuality.EXCELLENT;
            case ConnectivityManager.TYPE_MOBILE:
                // Évaluer selon le type de réseau mobile
                switch (networkInfo.getSubtype()) {
                    case 13: // LTE
                        return ConnectionQuality.GOOD;
                    case 15: // HSPA+
                        return ConnectionQuality.FAIR;
                    case 3: // UMTS
                        return ConnectionQuality.FAIR;
                    case 1: // GPRS
                        return ConnectionQuality.POOR;
                    default:
                        return ConnectionQuality.FAIR;
                }
            default:
                return ConnectionQuality.FAIR;
        }
    }
    
    /**
     * Déterminer si la connexion est suffisante pour la synchronisation
     */
    public static boolean isConnectionGoodForSync(Context context) {
        ConnectionQuality quality = getConnectionQuality(context);
        return quality != ConnectionQuality.OFFLINE && quality != ConnectionQuality.POOR;
    }
    
    // ==================== RECOMMANDATIONS DE SYNCHRONISATION ====================
    
    /**
     * Obtenir des recommandations pour la synchronisation basées sur la qualité de connexion
     */
    public static SyncRecommendation getSyncRecommendation(Context context) {
        ConnectionQuality quality = getConnectionQuality(context);
        String connectionType = getConnectionType(context);
        
        switch (quality) {
            case OFFLINE:
                return new SyncRecommendation(
                    false, 
                    "Hors ligne", 
                    "La synchronisation sera effectuée automatiquement dès la reconnexion"
                );
                
            case POOR:
                return new SyncRecommendation(
                    false, 
                    "Connexion faible (" + connectionType + ")", 
                    "Attendez une meilleure connexion pour synchroniser ou utilisez le WiFi"
                );
                
            case FAIR:
                return new SyncRecommendation(
                    true, 
                    "Connexion acceptable (" + connectionType + ")", 
                    "La synchronisation peut prendre plus de temps que d'habitude"
                );
                
            case GOOD:
                return new SyncRecommendation(
                    true, 
                    "Bonne connexion (" + connectionType + ")", 
                    "Conditions optimales pour la synchronisation"
                );
                
            case EXCELLENT:
                return new SyncRecommendation(
                    true, 
                    "Excellente connexion (" + connectionType + ")", 
                    "Conditions parfaites pour la synchronisation"
                );
                
            default:
                return new SyncRecommendation(
                    false, 
                    "Connexion inconnue", 
                    "Impossible d'évaluer la qualité de la connexion"
                );
        }
    }
    
    // ==================== CLASSES UTILITAIRES ====================
    
    public enum ConnectionQuality {
        OFFLINE,    // Pas de connexion
        POOR,       // Connexion très lente (2G)
        FAIR,       // Connexion acceptable (3G)
        GOOD,       // Bonne connexion (4G)
        EXCELLENT   // Excellente connexion (WiFi/Ethernet)
    }
    
    public static class SyncRecommendation {
        public final boolean shouldSync;
        public final String connectionStatus;
        public final String message;
        
        public SyncRecommendation(boolean shouldSync, String connectionStatus, String message) {
            this.shouldSync = shouldSync;
            this.connectionStatus = connectionStatus;
            this.message = message;
        }
    }
    
    // ==================== MÉTHODES DE DEBUG ====================
    
    /**
     * Obtenir des informations détaillées sur la connexion (pour debug)
     */
    public static String getDetailedConnectionInfo(Context context) {
        StringBuilder info = new StringBuilder();
        
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        
        if (cm == null) {
            return "ConnectivityManager non disponible";
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_NETWORK_STATE) 
                != PackageManager.PERMISSION_GRANTED) {
                return "Permission ACCESS_NETWORK_STATE non accordée";
            }
        }
        
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        
        if (networkInfo == null) {
            return "Aucune information de réseau disponible";
        }
        
        info.append("Type: ").append(networkInfo.getTypeName()).append("\n");
        info.append("Sous-type: ").append(networkInfo.getSubtypeName()).append("\n");
        info.append("Connecté: ").append(networkInfo.isConnected()).append("\n");
        info.append("Disponible: ").append(networkInfo.isAvailable()).append("\n");
        info.append("État: ").append(networkInfo.getState()).append("\n");
        info.append("Détaillé: ").append(networkInfo.getDetailedState()).append("\n");
        
        return info.toString();
    }
}
