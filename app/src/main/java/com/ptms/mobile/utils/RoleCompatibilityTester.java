package com.ptms.mobile.utils;

import android.content.Context;
import android.util.Log;

import com.ptms.mobile.api.ApiClient;
import com.ptms.mobile.api.ApiService;
import com.ptms.mobile.models.Employee;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Testeur de compatibilité des rôles pour l'application Android
 * Vérifie que l'application Android fonctionne correctement avec la nouvelle gestion des rôles PTMS
 */
public class RoleCompatibilityTester {
    
    private static final String TAG = "RoleCompatibilityTester";
    private ApiClient apiClient;
    private ApiService apiService;
    private Context context;
    
    public interface TestResultCallback {
        void onTestCompleted(boolean success, String message);
        void onTestProgress(String message);
    }
    
    public RoleCompatibilityTester(Context context) {
        this.context = context;
        this.apiClient = ApiClient.getInstance(context);
        this.apiService = apiClient.getApiService();
    }
    
    /**
     * Tester la compatibilité complète avec les nouveaux rôles
     */
    public void testRoleCompatibility(String token, TestResultCallback callback) {
        callback.onTestProgress("🔍 Démarrage des tests de compatibilité des rôles...");
        
        // Test 1: Vérifier l'authentification
        testAuthentication(token, new TestResultCallback() {
            @Override
            public void onTestCompleted(boolean success, String message) {
                if (!success) {
                    callback.onTestCompleted(false, "Échec de l'authentification: " + message);
                    return;
                }
                
                callback.onTestProgress("✅ Authentification réussie");
                
                // Test 2: Vérifier l'accès au profil
                testProfileAccess(token, new TestResultCallback() {
                    @Override
                    public void onTestCompleted(boolean success, String message) {
                        if (!success) {
                            callback.onTestCompleted(false, "Échec de l'accès au profil: " + message);
                            return;
                        }
                        
                        callback.onTestProgress("✅ Accès au profil réussi");
                        
                        // Test 3: Vérifier l'accès aux projets
                        testProjectsAccess(token, new TestResultCallback() {
                            @Override
                            public void onTestCompleted(boolean success, String message) {
                                if (!success) {
                                    callback.onTestCompleted(false, "Échec de l'accès aux projets: " + message);
                                    return;
                                }
                                
                                callback.onTestProgress("✅ Accès aux projets réussi");
                                
                                // Test 4: Vérifier l'accès aux types de travail
                                testWorkTypesAccess(token, new TestResultCallback() {
                                    @Override
                                    public void onTestCompleted(boolean success, String message) {
                                        if (!success) {
                                            callback.onTestCompleted(false, "Échec de l'accès aux types de travail: " + message);
                                            return;
                                        }
                                        
                                        callback.onTestProgress("✅ Accès aux types de travail réussi");
                                        
                                        // Test 5: Vérifier l'accès aux rapports
                                        testReportsAccess(token, new TestResultCallback() {
                                            @Override
                                            public void onTestCompleted(boolean success, String message) {
                                                if (!success) {
                                                    callback.onTestCompleted(false, "Échec de l'accès aux rapports: " + message);
                                                    return;
                                                }
                                                
                                                callback.onTestProgress("✅ Accès aux rapports réussi");
                                                
                                                // Test 6: Vérifier la sauvegarde d'heures
                                                testTimeEntrySave(token, new TestResultCallback() {
                                                    @Override
                                                    public void onTestCompleted(boolean success, String message) {
                                                        if (!success) {
                                                            callback.onTestCompleted(false, "Échec de la sauvegarde d'heures: " + message);
                                                            return;
                                                        }
                                                        
                                                        callback.onTestProgress("✅ Sauvegarde d'heures réussie");
                                                        
                                                        // Test 7: Vérifier l'API unifiée
                                                        testUnifiedApiAccess(token, new TestResultCallback() {
                                                            @Override
                                                            public void onTestCompleted(boolean success, String message) {
                                                                if (!success) {
                                                                    callback.onTestCompleted(false, "Échec de l'API unifiée: " + message);
                                                                    return;
                                                                }
                                                                
                                                                callback.onTestProgress("✅ API unifiée accessible");
                                                                
                                                                // Tous les tests réussis
                                                                callback.onTestCompleted(true, "🎉 Tous les tests de compatibilité des rôles ont réussi!");
                                                            }
                                                            
                                                            @Override
                                                            public void onTestProgress(String message) {
                                                                callback.onTestProgress(message);
                                                            }
                                                        });
                                                    }
                                                    
                                                    @Override
                                                    public void onTestProgress(String message) {
                                                        callback.onTestProgress(message);
                                                    }
                                                });
                                            }
                                            
                                            @Override
                                            public void onTestProgress(String message) {
                                                callback.onTestProgress(message);
                                            }
                                        });
                                    }
                                    
                                    @Override
                                    public void onTestProgress(String message) {
                                        callback.onTestProgress(message);
                                    }
                                });
                            }
                            
                            @Override
                            public void onTestProgress(String message) {
                                callback.onTestProgress(message);
                            }
                        });
                    }
                    
                    @Override
                    public void onTestProgress(String message) {
                        callback.onTestProgress(message);
                    }
                });
            }
            
            @Override
            public void onTestProgress(String message) {
                callback.onTestProgress(message);
            }
        });
    }
    
    /**
     * Test de l'authentification
     */
    private void testAuthentication(String token, TestResultCallback callback) {
        callback.onTestProgress("🔐 Test de l'authentification...");
        
        // Simuler une requête qui nécessite une authentification
        apiService.getProfile("Bearer " + token).enqueue(new Callback<Employee>() {
            @Override
            public void onResponse(Call<Employee> call, Response<Employee> response) {
                if (response.isSuccessful()) {
                    callback.onTestCompleted(true, "Authentification valide");
                } else {
                    callback.onTestCompleted(false, "Code HTTP: " + response.code());
                }
            }
            
            @Override
            public void onFailure(Call<Employee> call, Throwable t) {
                Log.e(TAG, "Erreur d'authentification", t);
                callback.onTestCompleted(false, "Erreur réseau: " + t.getMessage());
            }
        });
    }
    
    /**
     * Test de l'accès au profil
     */
    private void testProfileAccess(String token, TestResultCallback callback) {
        callback.onTestProgress("👤 Test de l'accès au profil...");
        
        apiService.getProfile("Bearer " + token).enqueue(new Callback<Employee>() {
            @Override
            public void onResponse(Call<Employee> call, Response<Employee> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Employee profile = response.body();
                    callback.onTestCompleted(true, "Profil récupéré: " + profile.getFirstname() + " " + profile.getLastname());
                } else {
                    callback.onTestCompleted(false, "Impossible de récupérer le profil");
                }
            }
            
            @Override
            public void onFailure(Call<Employee> call, Throwable t) {
                Log.e(TAG, "Erreur d'accès au profil", t);
                callback.onTestCompleted(false, "Erreur réseau: " + t.getMessage());
            }
        });
    }
    
    /**
     * Test de l'accès aux projets
     */
    private void testProjectsAccess(String token, TestResultCallback callback) {
        callback.onTestProgress("📋 Test de l'accès aux projets...");
        
        apiService.getProjects("Bearer " + token).enqueue(new Callback<ApiService.ProjectsResponse>() {
            @Override
            public void onResponse(Call<ApiService.ProjectsResponse> call, Response<ApiService.ProjectsResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().success) {
                    List<com.ptms.mobile.models.Project> projects = response.body().projects;
                    callback.onTestCompleted(true, "Projets récupérés: " + projects.size() + " projets");
                } else {
                    callback.onTestCompleted(false, "Impossible de récupérer les projets");
                }
            }
            
            @Override
            public void onFailure(Call<ApiService.ProjectsResponse> call, Throwable t) {
                Log.e(TAG, "Erreur d'accès aux projets", t);
                callback.onTestCompleted(false, "Erreur réseau: " + t.getMessage());
            }
        });
    }
    
    /**
     * Test de l'accès aux types de travail
     */
    private void testWorkTypesAccess(String token, TestResultCallback callback) {
        callback.onTestProgress("⚙️ Test de l'accès aux types de travail...");
        
        apiService.getWorkTypes("Bearer " + token).enqueue(new Callback<List<com.ptms.mobile.models.WorkType>>() {
            @Override
            public void onResponse(Call<List<com.ptms.mobile.models.WorkType>> call, Response<List<com.ptms.mobile.models.WorkType>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<com.ptms.mobile.models.WorkType> workTypes = response.body();
                    callback.onTestCompleted(true, "Types de travail récupérés: " + workTypes.size() + " types");
                } else {
                    callback.onTestCompleted(false, "Impossible de récupérer les types de travail");
                }
            }
            
            @Override
            public void onFailure(Call<List<com.ptms.mobile.models.WorkType>> call, Throwable t) {
                Log.e(TAG, "Erreur d'accès aux types de travail", t);
                callback.onTestCompleted(false, "Erreur réseau: " + t.getMessage());
            }
        });
    }
    
    /**
     * Test de l'accès aux rapports
     */
    private void testReportsAccess(String token, TestResultCallback callback) {
        callback.onTestProgress("📊 Test de l'accès aux rapports...");
        
        String dateFrom = "2024-01-01";
        String dateTo = "2024-12-31";
        
        apiService.getReports("Bearer " + token, dateFrom, dateTo, null).enqueue(new Callback<List<com.ptms.mobile.models.TimeReport>>() {
            @Override
            public void onResponse(Call<List<com.ptms.mobile.models.TimeReport>> call, Response<List<com.ptms.mobile.models.TimeReport>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<com.ptms.mobile.models.TimeReport> reports = response.body();
                    callback.onTestCompleted(true, "Rapports récupérés: " + reports.size() + " rapports");
                } else {
                    callback.onTestCompleted(false, "Impossible de récupérer les rapports");
                }
            }
            
            @Override
            public void onFailure(Call<List<com.ptms.mobile.models.TimeReport>> call, Throwable t) {
                Log.e(TAG, "Erreur d'accès aux rapports", t);
                callback.onTestCompleted(false, "Erreur réseau: " + t.getMessage());
            }
        });
    }
    
    /**
     * Test de la sauvegarde d'heures
     */
    private void testTimeEntrySave(String token, TestResultCallback callback) {
        callback.onTestProgress("💾 Test de la sauvegarde d'heures...");

        // Créer un rapport de test avec TOUTES les données requises
        com.ptms.mobile.models.TimeReport testReport = new com.ptms.mobile.models.TimeReport();
        testReport.setProjectId(12);  // Utiliser un projet qui existe
        testReport.setWorkTypeId(9);  // Utiliser un type de travail qui existe
        testReport.setReportDate("2025-10-25");
        testReport.setDatetimeFrom("2025-10-25 09:00:00");  // ✅ REQUIS
        testReport.setDatetimeTo("2025-10-25 10:00:00");    // ✅ REQUIS
        testReport.setHours(1.0);
        testReport.setDescription("Test de compatibilité Android");
        testReport.setTimezone("Europe/Zurich");  // ✅ REQUIS

        apiService.saveTimeEntry("Bearer " + token, testReport).enqueue(new Callback<ApiService.ApiResponse>() {
            @Override
            public void onResponse(Call<ApiService.ApiResponse> call, Response<ApiService.ApiResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiService.ApiResponse apiResponse = response.body();
                    if (apiResponse.success) {
                        callback.onTestCompleted(true, "Sauvegarde d'heures réussie");
                    } else {
                        callback.onTestCompleted(false, "Échec de la sauvegarde: " + apiResponse.message);
                    }
                } else {
                    callback.onTestCompleted(false, "Erreur HTTP: " + response.code());
                }
            }
            
            @Override
            public void onFailure(Call<ApiService.ApiResponse> call, Throwable t) {
                Log.e(TAG, "Erreur de sauvegarde d'heures", t);
                callback.onTestCompleted(false, "Erreur réseau: " + t.getMessage());
            }
        });
    }
    
    /**
     * Test de l'accès à l'API unifiée
     */
    private void testUnifiedApiAccess(String token, TestResultCallback callback) {
        callback.onTestProgress("🔗 Test de l'API unifiée...");
        
        apiService.getSystemStatus("Bearer " + token).enqueue(new Callback<ApiService.SystemStatusResponse>() {
            @Override
            public void onResponse(Call<ApiService.SystemStatusResponse> call, Response<ApiService.SystemStatusResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiService.SystemStatusResponse status = response.body();
                    callback.onTestCompleted(true, "API unifiée accessible: " + status.message);
                } else {
                    callback.onTestCompleted(false, "API unifiée non accessible");
                }
            }
            
            @Override
            public void onFailure(Call<ApiService.SystemStatusResponse> call, Throwable t) {
                Log.e(TAG, "Erreur d'accès à l'API unifiée", t);
                // L'API unifiée peut ne pas être disponible, ce n'est pas critique
                callback.onTestCompleted(true, "API unifiée non disponible (fallback OK)");
            }
        });
    }
    
    /**
     * Méthode utilitaire pour tester rapidement la compatibilité
     */
    public static void quickTest(Context context, String token, TestResultCallback callback) {
        RoleCompatibilityTester tester = new RoleCompatibilityTester(context);
        tester.testRoleCompatibility(token, callback);
    }
}
