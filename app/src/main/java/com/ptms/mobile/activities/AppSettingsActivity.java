package com.ptms.mobile.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.ptms.mobile.R;
import com.ptms.mobile.utils.SettingsManager;

/**
 * Activité de configuration des paramètres généraux de l'application
 */
public class AppSettingsActivity extends AppCompatActivity {
    
    private SettingsManager settingsManager;
    
    private EditText etServerUrl;
    private TextView tvFullUrlPreview;
    private CheckBox cbIgnoreSsl;
    private SeekBar sbTimeout;
    private TextView tvTimeoutValue;
    private CheckBox cbDebugMode;
    private CheckBox cbEnableChat;
    private CheckBox cbEnableChatPolling;
    private RadioGroup rgChatVersion;
    private RadioButton rbChatV1;
    private RadioButton rbChatV2;
    private Button btnSave;
    private Button btnReset;
    private Button btnTestConnection;
    private Button btnTestBaseUrl;
    private Button btnDiagnostic;
    private Button btnDevMode;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings_simple);
        
        settingsManager = new SettingsManager(this);
        initViews();
        // setupToolbar(); // Temporairement désactivé
        loadSettings();
        setupListeners();
        
        Toast.makeText(this, "Page de paramètres ouverte", Toast.LENGTH_SHORT).show();
    }
    
    private void initViews() {
        try {
            etServerUrl = findViewById(R.id.et_server_url);
            tvFullUrlPreview = findViewById(R.id.tv_full_url_preview);
            cbIgnoreSsl = findViewById(R.id.cb_ignore_ssl);
            sbTimeout = findViewById(R.id.sb_timeout);
            tvTimeoutValue = findViewById(R.id.tv_timeout_value);
            cbDebugMode = findViewById(R.id.cb_debug_mode);
            cbEnableChat = findViewById(R.id.cb_enable_chat);
            cbEnableChatPolling = findViewById(R.id.cb_enable_chat_polling);
            rgChatVersion = findViewById(R.id.rg_chat_version);
            rbChatV1 = findViewById(R.id.rb_chat_v1);
            rbChatV2 = findViewById(R.id.rb_chat_v2);
            btnSave = findViewById(R.id.btn_save_settings);
            btnReset = findViewById(R.id.btn_reset_settings);
            btnTestConnection = findViewById(R.id.btn_test_connection);
            btnTestBaseUrl = findViewById(R.id.btn_test_base_url);
            btnDiagnostic = findViewById(R.id.btn_diagnostic);
            btnDevMode = findViewById(R.id.btn_dev_mode);

            // Vérifications de nullité
            if (etServerUrl == null) Log.w("SETTINGS", "etServerUrl est null");
            if (cbIgnoreSsl == null) Log.w("SETTINGS", "cbIgnoreSsl est null");
            if (sbTimeout == null) Log.w("SETTINGS", "sbTimeout est null");
            if (tvTimeoutValue == null) Log.w("SETTINGS", "tvTimeoutValue est null");
            if (cbDebugMode == null) Log.w("SETTINGS", "cbDebugMode est null");
            if (btnSave == null) Log.w("SETTINGS", "btnSave est null");
            if (btnReset == null) Log.w("SETTINGS", "btnReset est null");
            if (btnTestConnection == null) Log.w("SETTINGS", "btnTestConnection est null");
            if (btnTestBaseUrl == null) Log.w("SETTINGS", "btnTestBaseUrl est null");
            
            Toast.makeText(this, "Vues initialisées", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e("SETTINGS", "Erreur initialisation vues", e);
            Toast.makeText(this, "Erreur initialisation: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    
    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Paramètres");
        }
    }
    
    private void loadSettings() {
        Log.d("SETTINGS", "Chargement des paramètres");
        String serverUrlRaw = settingsManager.getServerUrlRaw(); // URL brute (juste IP)
        String serverUrlFull = settingsManager.getServerUrl();   // URL complète (avec /api/)
        boolean ignoreSsl = settingsManager.isIgnoreSsl();
        Log.d("SETTINGS", "URL du serveur (brute): " + serverUrlRaw);
        Log.d("SETTINGS", "URL du serveur (complète): " + serverUrlFull);
        Log.d("SETTINGS", "Ignorer SSL: " + ignoreSsl);

        if (etServerUrl != null) etServerUrl.setText(serverUrlRaw);
        if (tvFullUrlPreview != null) tvFullUrlPreview.setText("URL complète: " + serverUrlFull);
        if (cbIgnoreSsl != null) cbIgnoreSsl.setChecked(ignoreSsl);
        if (sbTimeout != null) sbTimeout.setProgress(settingsManager.getTimeout());
        if (tvTimeoutValue != null) tvTimeoutValue.setText(settingsManager.getTimeout() + " secondes");
        if (cbDebugMode != null) cbDebugMode.setChecked(settingsManager.isDebugMode());
        if (cbEnableChat != null) cbEnableChat.setChecked(settingsManager.isChatEnabled());
        if (cbEnableChatPolling != null) cbEnableChatPolling.setChecked(settingsManager.isChatPollingEnabled());

        // Charger la version du chat
        int chatVersion = settingsManager.getChatVersion();
        if (chatVersion == SettingsManager.CHAT_VERSION_WEBSOCKET) {
            if (rbChatV2 != null) rbChatV2.setChecked(true);
        } else {
            if (rbChatV1 != null) rbChatV1.setChecked(true);
        }

        Log.d("SETTINGS", "Paramètres chargés");
    }
    
    private void setupListeners() {
        // Listener pour mettre à jour le preview de l'URL en temps réel
        if (etServerUrl != null) {
            etServerUrl.addTextChangedListener(new android.text.TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    // Créer un SettingsManager temporaire pour normaliser l'URL
                    String rawUrl = s.toString().trim();
                    if (rawUrl.isEmpty()) {
                        if (tvFullUrlPreview != null) {
                            tvFullUrlPreview.setText("URL complète: (vide)");
                        }
                    } else {
                        // Normaliser l'URL pour le preview
                        String normalized = normalizeUrlForPreview(rawUrl);
                        if (tvFullUrlPreview != null) {
                            tvFullUrlPreview.setText("URL complète: " + normalized);
                        }
                    }
                }

                @Override
                public void afterTextChanged(android.text.Editable s) {}
            });
        }

        // Vérifier que sbTimeout n'est pas null avant d'appeler setOnSeekBarChangeListener
        if (sbTimeout != null) {
            sbTimeout.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (tvTimeoutValue != null) {
                    tvTimeoutValue.setText(progress + " secondes");
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        }
        
        if (btnSave != null) {
            btnSave.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    saveSettings();
                }
            });
        }
        
        if (btnReset != null) {
            btnReset.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    resetSettings();
                }
            });
        }
        
        if (btnTestConnection != null) {
            btnTestConnection.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    testConnection();
                }
            });
        }
        
        if (btnTestBaseUrl != null) {
            btnTestBaseUrl.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    testBaseUrl();
                }
            });
        }

        if (btnDiagnostic != null) {
            btnDiagnostic.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(new Intent(AppSettingsActivity.this, SystemDiagnosticsActivity.class));
                }
            });
        }

        if (btnDevMode != null) {
            btnDevMode.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(new Intent(AppSettingsActivity.this, DeveloperToolsActivity.class));
                }
            });
        }
    }
    
    private void saveSettings() {
        if (etServerUrl == null) {
            Toast.makeText(this, "Erreur: Champ URL serveur non trouvé", Toast.LENGTH_LONG).show();
            return;
        }
        String serverUrl = etServerUrl.getText().toString().trim();
        
        // Validation de l'URL
        if (!settingsManager.isUrlValid(serverUrl)) {
            etServerUrl.setError("URL invalide. Format requis: http://... ou https://...");
            etServerUrl.requestFocus();
            return;
        }
        
        // Sauvegarde des paramètres
        settingsManager.setServerUrl(serverUrl);
        settingsManager.setIgnoreSsl(cbIgnoreSsl != null && cbIgnoreSsl.isChecked());
        settingsManager.setTimeout(sbTimeout != null ? sbTimeout.getProgress() : 30);
        settingsManager.setDebugMode(cbDebugMode != null && cbDebugMode.isChecked());
        settingsManager.setChatEnabled(cbEnableChat != null && cbEnableChat.isChecked());
        settingsManager.setChatPollingEnabled(cbEnableChatPolling != null && cbEnableChatPolling.isChecked());

        // Sauvegarder la version du chat
        if (rgChatVersion != null) {
            int selectedId = rgChatVersion.getCheckedRadioButtonId();
            if (selectedId == R.id.rb_chat_v2) {
                settingsManager.setChatVersion(SettingsManager.CHAT_VERSION_WEBSOCKET);
                Toast.makeText(this, "Paramètres sauvegardés - Chat V2 (WebSocket) activé", Toast.LENGTH_LONG).show();
            } else {
                settingsManager.setChatVersion(SettingsManager.CHAT_VERSION_POLLING);
                Toast.makeText(this, "Paramètres sauvegardés - Chat V1 (Polling) activé", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Paramètres sauvegardés", Toast.LENGTH_SHORT).show();
        }

        // ✅ IMPORTANT: Reconfigurer ApiClient avec les nouveaux paramètres
        try {
            com.ptms.mobile.api.ApiClient apiClient = com.ptms.mobile.api.ApiClient.getInstance(this);
            apiClient.refreshConfiguration();
            android.util.Log.d("SETTINGS", "✅ ApiClient reconfiguré avec les nouveaux paramètres");
        } catch (Exception e) {
            android.util.Log.e("SETTINGS", "Erreur lors de la reconfiguration ApiClient", e);
        }

        // Notifier que les paramètres ont été mis à jour (pour les autres activités)
        setResult(RESULT_OK);
        
        // Ne pas fermer automatiquement la page - laisser l'utilisateur rester
    }
    
    private void resetSettings() {
        settingsManager.resetToDefaults();
        loadSettings();
        Toast.makeText(this, "Paramètres réinitialisés", Toast.LENGTH_SHORT).show();
    }
    
    private void testConnection() {
        if (etServerUrl == null) {
            Toast.makeText(this, "Erreur: Champ URL serveur non trouvé", Toast.LENGTH_LONG).show();
            return;
        }
        String serverUrlRaw = etServerUrl.getText().toString().trim();

        if (!settingsManager.isUrlValid(serverUrlRaw)) {
            etServerUrl.setError("URL invalide");
            etServerUrl.requestFocus();
            return;
        }

        // IMPORTANT: Normaliser l'URL comme le fait SettingsManager
        String serverUrl = normalizeUrlForPreview(serverUrlRaw);

        Log.d("SETTINGS", "Test de connexion vers: " + serverUrl);
        Toast.makeText(this, "Test de connexion en cours...", Toast.LENGTH_SHORT).show();
        
        if (btnTestConnection != null) btnTestConnection.setEnabled(false);
        
        // Test réel avec les paramètres configurés
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // Utiliser les paramètres actuels (pas encore sauvegardés)
                    boolean ignoreSsl = cbIgnoreSsl != null && cbIgnoreSsl.isChecked();
                    int timeout = sbTimeout != null ? sbTimeout.getProgress() : 30;
                    
                    Log.d("SETTINGS", "Test avec paramètres - Ignorer SSL: " + ignoreSsl + ", Timeout: " + timeout);
                    
                    okhttp3.OkHttpClient.Builder clientBuilder = new okhttp3.OkHttpClient.Builder()
                            .connectTimeout(timeout, java.util.concurrent.TimeUnit.SECONDS)
                            .readTimeout(timeout, java.util.concurrent.TimeUnit.SECONDS);
                    
                    // Configuration SSL si nécessaire
                    if (ignoreSsl) {
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
                            clientBuilder.sslSocketFactory(sslSocketFactory, (javax.net.ssl.X509TrustManager) trustAllCerts[0]);
                            clientBuilder.hostnameVerifier(new javax.net.ssl.HostnameVerifier() {
                                @Override
                                public boolean verify(String hostname, javax.net.ssl.SSLSession session) {
                                    return true; // Accepter tous les hostnames
                                }
                            });
                            
                            Log.d("SETTINGS", "SSL ignoré pour le test");
                        } catch (Exception sslException) {
                            Log.e("SETTINGS", "Erreur configuration SSL pour test", sslException);
                        }
                    }
                    
                    okhttp3.OkHttpClient client = clientBuilder.build();
                    
                    okhttp3.Request request = new okhttp3.Request.Builder()
                            .url(serverUrl + "login.php")
                            .post(okhttp3.RequestBody.create("{}", okhttp3.MediaType.get("application/json; charset=utf-8")))
                            .addHeader("Content-Type", "application/json")
                            .addHeader("Accept", "application/json")
                            .addHeader("User-Agent", "PTMS-Mobile-App/1.0")
                            .build();
                    
                    okhttp3.Response response = client.newCall(request).execute();
                    String responseBody = response.body().string();
                    
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (btnTestConnection != null) btnTestConnection.setEnabled(true);
                            String message = "Test OK - Code: " + response.code();
                            
                            // Analyser la réponse
                            if (responseBody.contains("<html")) {
                                message += " (Retourne du HTML)";
                                Log.e("SETTINGS", "PROBLÈME: Le serveur retourne du HTML au lieu de JSON!");
                                
                                // Analyser le type d'erreur HTML
                                if (responseBody.contains("404") || responseBody.contains("Not Found")) {
                                    message += " - Endpoint non trouvé (404)";
                                } else if (responseBody.contains("403") || responseBody.contains("Forbidden")) {
                                    message += " - Accès interdit (403)";
                                } else if (responseBody.contains("401") || responseBody.contains("Unauthorized")) {
                                    message += " - Non autorisé (401)";
                                } else if (responseBody.contains("500") || responseBody.contains("Internal Server Error")) {
                                    message += " - Erreur serveur (500)";
                                } else {
                                    message += " - Page d'erreur serveur";
                                }
                            } else if (responseBody.contains("{") && responseBody.contains("}")) {
                                message += " (Retourne du JSON)";
                                Log.d("SETTINGS", "SUCCÈS: Le serveur retourne du JSON");
                            } else {
                                message += " (Réponse inconnue)";
                                Log.w("SETTINGS", "Réponse inattendue du serveur");
                            }
                            
                            // Logs détaillés
                            Log.d("SETTINGS", "Headers de réponse:");
                            for (String headerName : response.headers().names()) {
                                Log.d("SETTINGS", "  " + headerName + ": " + response.header(headerName));
                            }
                            Log.d("SETTINGS", "Body de réponse (200 premiers caractères): " + responseBody.substring(0, Math.min(200, responseBody.length())));
                            
                            Toast.makeText(AppSettingsActivity.this, message, Toast.LENGTH_LONG).show();
                        }
                    });
                    
                } catch (Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (btnTestConnection != null) btnTestConnection.setEnabled(true);
                            Log.e("SETTINGS", "Test échoué", e);
                            String errorMsg = "Test échoué: ";
                            if (e.getMessage().contains("SSL") || e.getMessage().contains("certificate")) {
                                errorMsg += "Erreur SSL - Activez 'Ignorer SSL'";
                            } else if (e.getMessage().contains("timeout")) {
                                errorMsg += "Timeout - Augmentez le délai";
                            } else if (e.getMessage().contains("UnknownHostException")) {
                                errorMsg += "Serveur non accessible";
                            } else {
                                errorMsg += e.getMessage();
                            }
                            Toast.makeText(AppSettingsActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        }).start();
    }
    
    private void testBaseUrl() {
        if (etServerUrl == null) {
            Toast.makeText(this, "Erreur: Champ URL serveur non trouvé", Toast.LENGTH_LONG).show();
            return;
        }
        String serverUrlRaw = etServerUrl.getText().toString().trim();

        if (!settingsManager.isUrlValid(serverUrlRaw)) {
            etServerUrl.setError("URL invalide");
            etServerUrl.requestFocus();
            return;
        }

        // IMPORTANT: Normaliser l'URL comme le fait SettingsManager
        String serverUrl = normalizeUrlForPreview(serverUrlRaw);

        Log.d("SETTINGS", "Test URL de base vers: " + serverUrl);
        Toast.makeText(this, "Test URL de base en cours...", Toast.LENGTH_SHORT).show();
        
        if (btnTestBaseUrl != null) btnTestBaseUrl.setEnabled(false);
        
        // Test de l'URL de base seulement
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    boolean ignoreSsl = cbIgnoreSsl != null && cbIgnoreSsl.isChecked();
                    int timeout = sbTimeout != null ? sbTimeout.getProgress() : 30;
                    
                    okhttp3.OkHttpClient.Builder clientBuilder = new okhttp3.OkHttpClient.Builder()
                            .connectTimeout(timeout, java.util.concurrent.TimeUnit.SECONDS)
                            .readTimeout(timeout, java.util.concurrent.TimeUnit.SECONDS);
                    
                    // Configuration SSL si nécessaire (même code que testConnection)
                    if (ignoreSsl) {
                        try {
                            javax.net.ssl.TrustManager[] trustAllCerts = new javax.net.ssl.TrustManager[] {
                                new javax.net.ssl.X509TrustManager() {
                                    @Override
                                    public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) {}
                                    @Override
                                    public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) {}
                                    @Override
                                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                                        return new java.security.cert.X509Certificate[]{};
                                    }
                                }
                            };
                            javax.net.ssl.SSLContext sslContext = javax.net.ssl.SSLContext.getInstance("SSL");
                            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
                            javax.net.ssl.SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
                            clientBuilder.sslSocketFactory(sslSocketFactory, (javax.net.ssl.X509TrustManager) trustAllCerts[0]);
                            clientBuilder.hostnameVerifier((hostname, session) -> true);
                        } catch (Exception sslException) {
                            Log.e("SETTINGS", "Erreur configuration SSL pour test base URL", sslException);
                        }
                    }
                    
                    okhttp3.OkHttpClient client = clientBuilder.build();
                    
                    // Test simple GET sur l'URL de base
                    okhttp3.Request request = new okhttp3.Request.Builder()
                            .url(serverUrl)
                            .get()
                            .addHeader("Accept", "text/html,application/json,*/*")
                            .addHeader("User-Agent", "PTMS-Mobile-App/1.0")
                            .build();
                    
                    okhttp3.Response response = client.newCall(request).execute();
                    String responseBody = response.body().string();
                    
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (btnTestBaseUrl != null) btnTestBaseUrl.setEnabled(true);
                            String message = "URL de base - Code: " + response.code();
                            
                            Log.d("SETTINGS", "Test URL de base - Code: " + response.code());
                            Log.d("SETTINGS", "Headers de réponse URL de base:");
                            for (String headerName : response.headers().names()) {
                                Log.d("SETTINGS", "  " + headerName + ": " + response.header(headerName));
                            }
                            Log.d("SETTINGS", "Body URL de base (200 premiers caractères): " + responseBody.substring(0, Math.min(200, responseBody.length())));
                            
                            if (response.code() == 200) {
                                message += " - Serveur accessible";
                            } else if (response.code() == 404) {
                                message += " - Page non trouvée";
                            } else if (response.code() == 403) {
                                message += " - Accès interdit";
                            } else if (response.code() == 401) {
                                message += " - Non autorisé";
                            }
                            
                            Toast.makeText(AppSettingsActivity.this, message, Toast.LENGTH_LONG).show();
                        }
                    });
                    
                } catch (Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (btnTestBaseUrl != null) btnTestBaseUrl.setEnabled(true);
                            Log.e("SETTINGS", "Test URL de base échoué", e);
                            Toast.makeText(AppSettingsActivity.this, "Test URL de base échoué: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        }).start();
    }
    
    /**
     * Normalise l'URL pour le preview (même logique que SettingsManager)
     */
    private String normalizeUrlForPreview(String url) {
        if (url == null || url.isEmpty()) {
            return "";
        }

        url = url.trim();

        // Si pas de protocole, ajouter https://
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://" + url;
        }

        // Retirer le slash final s'il existe
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }

        // Ajouter /api/ si pas déjà là
        if (!url.endsWith("/api")) {
            url = url + "/api";
        }

        // Ajouter le slash final
        url = url + "/";

        return url;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            Toast.makeText(this, "Retour vers l'activité précédente", Toast.LENGTH_SHORT).show();
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    @Override
    public void onBackPressed() {
        Toast.makeText(this, "Retour avec bouton retour", Toast.LENGTH_SHORT).show();
        super.onBackPressed();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        Toast.makeText(this, "SettingsActivity onResume", Toast.LENGTH_SHORT).show();
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        Toast.makeText(this, "SettingsActivity onPause", Toast.LENGTH_SHORT).show();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        Toast.makeText(this, "SettingsActivity onDestroy", Toast.LENGTH_SHORT).show();
    }
}
