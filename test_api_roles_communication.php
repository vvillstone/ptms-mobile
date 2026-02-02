<?php
/**
 * Script de test pour vérifier la communication Android-API PTMS
 * avec la nouvelle gestion des rôles
 */

// Configuration
$BASE_URL = 'https://192.168.188.28/api/';
$TEST_USERS = [
    [
        'email' => 'admin@ptms.com',
        'password' => 'admin123',
        'expected_role' => 1,
        'role_name' => 'ADMIN'
    ],
    [
        'email' => 'manager@ptms.com', 
        'password' => 'manager123',
        'expected_role' => 2,
        'role_name' => 'MANAGER'
    ],
    [
        'email' => 'employee@ptms.com',
        'password' => 'employee123', 
        'expected_role' => 3,
        'role_name' => 'EMPLOYEE'
    ],
    [
        'email' => 'viewer@ptms.com',
        'password' => 'viewer123',
        'expected_role' => 4,
        'role_name' => 'VIEWER'
    ]
];

$ENDPOINTS_TO_TEST = [
    'login' => 'POST',
    'projects' => 'GET',
    'work-types' => 'GET', 
    'time-entry' => 'POST',
    'reports' => 'GET',
    'profile' => 'GET',
    'system/status' => 'GET'
];

class ApiTester {
    private $baseUrl;
    private $results = [];
    
    public function __construct($baseUrl) {
        $this->baseUrl = rtrim($baseUrl, '/');
    }
    
    /**
     * Effectuer une requête HTTP
     */
    private function makeRequest($endpoint, $method = 'GET', $data = null, $token = null) {
        $url = $this->baseUrl . '/' . ltrim($endpoint, '/');
        
        $ch = curl_init();
        curl_setopt($ch, CURLOPT_URL, $url);
        curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
        curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false);
        curl_setopt($ch, CURLOPT_SSL_VERIFYHOST, false);
        curl_setopt($ch, CURLOPT_TIMEOUT, 30);
        
        $headers = [
            'Content-Type: application/json',
            'Accept: application/json'
        ];
        
        if ($token) {
            $headers[] = 'Authorization: Bearer ' . $token;
        }
        
        curl_setopt($ch, CURLOPT_HTTPHEADER, $headers);
        
        if ($method === 'POST') {
            curl_setopt($ch, CURLOPT_POST, true);
            if ($data) {
                curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($data));
            }
        }
        
        $response = curl_exec($ch);
        $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
        $error = curl_error($ch);
        curl_close($ch);
        
        return [
            'http_code' => $httpCode,
            'response' => $response,
            'error' => $error,
            'url' => $url
        ];
    }
    
    /**
     * Tester la connexion d'un utilisateur
     */
    public function testLogin($email, $password, $expectedRole) {
        echo "🔐 Test de connexion pour $email (rôle attendu: $expectedRole)...\n";
        
        $result = $this->makeRequest('login.php', 'POST', [
            'email' => $email,
            'password' => $password
        ]);
        
        $success = $result['http_code'] === 200 && empty($result['error']);
        $responseData = null;
        
        if ($success && $result['response']) {
            $responseData = json_decode($result['response'], true);
            $success = $responseData && $responseData['success'] === true;
        }
        
        if ($success && $responseData) {
            echo "✅ Connexion réussie\n";
            echo "   Token: " . substr($responseData['token'], 0, 20) . "...\n";
            
            if (isset($responseData['user'])) {
                echo "   Utilisateur: {$responseData['user']['firstname']} {$responseData['user']['lastname']}\n";
                echo "   Département: {$responseData['user']['department']}\n";
                echo "   Position: {$responseData['user']['position']}\n";
            }
            
            return [
                'success' => true,
                'token' => $responseData['token'],
                'user' => $responseData['user'] ?? null
            ];
        } else {
            echo "❌ Échec de connexion\n";
            echo "   Code HTTP: {$result['http_code']}\n";
            echo "   Erreur: {$result['error']}\n";
            echo "   Réponse: {$result['response']}\n";
            
            return [
                'success' => false,
                'error' => $result['error'] ?: $result['response']
            ];
        }
    }
    
    /**
     * Tester un endpoint avec un token
     */
    public function testEndpoint($endpoint, $method, $token, $testData = null) {
        echo "   📡 Test endpoint: $endpoint ($method)...\n";
        
        $result = $this->makeRequest($endpoint, $method, $testData, $token);
        
        $success = $result['http_code'] === 200 && empty($result['error']);
        $responseData = null;
        
        if ($success && $result['response']) {
            $responseData = json_decode($result['response'], true);
        }
        
        if ($success) {
            echo "     ✅ Succès (HTTP {$result['http_code']})\n";
            if ($responseData && isset($responseData['success'])) {
                echo "     📊 Success: " . ($responseData['success'] ? 'true' : 'false') . "\n";
            }
        } else {
            echo "     ❌ Échec (HTTP {$result['http_code']})\n";
            if ($result['error']) {
                echo "     🔴 Erreur: {$result['error']}\n";
            }
            if ($result['response']) {
                echo "     📄 Réponse: {$result['response']}\n";
            }
        }
        
        return [
            'success' => $success,
            'http_code' => $result['http_code'],
            'response' => $responseData
        ];
    }
    
    /**
     * Tester tous les endpoints pour un utilisateur
     */
    public function testUserEndpoints($user, $token) {
        echo "\n🧪 Test des endpoints pour {$user['email']} (rôle: {$user['role_name']})\n";
        echo str_repeat('-', 60) . "\n";
        
        $results = [];
        
        // Test des endpoints de base
        $endpoints = [
            'projects.php' => ['GET', null],
            'work-types.php' => ['GET', null],
            'reports.php' => ['GET', null],
            'profile.php' => ['GET', null]
        ];
        
        // Test spécifique pour time-entry (POST)
        $timeEntryData = [
            'project_id' => 1,
            'work_type_id' => 1,
            'date' => date('Y-m-d'),
            'hours' => 8.0,
            'description' => 'Test API Android'
        ];
        $endpoints['time-entry.php'] = ['POST', $timeEntryData];
        
        foreach ($endpoints as $endpoint => $config) {
            list($method, $data) = $config;
            $results[$endpoint] = $this->testEndpoint($endpoint, $method, $token, $data);
        }
        
        return $results;
    }
    
    /**
     * Générer le rapport de test
     */
    public function generateReport($allResults) {
        echo "\n" . str_repeat('=', 80) . "\n";
        echo "📋 RAPPORT DE TEST - Communication Android-API PTMS\n";
        echo str_repeat('=', 80) . "\n";
        
        $totalUsers = count($allResults);
        $successfulLogins = 0;
        $totalEndpoints = 0;
        $successfulEndpoints = 0;
        
        foreach ($allResults as $userEmail => $userResults) {
            echo "\n👤 Utilisateur: $userEmail\n";
            echo "   Rôle: {$userResults['role_name']}\n";
            
            if ($userResults['login']['success']) {
                $successfulLogins++;
                echo "   🔐 Connexion: ✅ Réussie\n";
                
                if (isset($userResults['endpoints'])) {
                    foreach ($userResults['endpoints'] as $endpoint => $result) {
                        $totalEndpoints++;
                        if ($result['success']) {
                            $successfulEndpoints++;
                            echo "   📡 $endpoint: ✅ OK\n";
                        } else {
                            echo "   📡 $endpoint: ❌ Échec (HTTP {$result['http_code']})\n";
                        }
                    }
                }
            } else {
                echo "   🔐 Connexion: ❌ Échec\n";
                echo "   📄 Erreur: {$userResults['login']['error']}\n";
            }
        }
        
        echo "\n" . str_repeat('-', 80) . "\n";
        echo "📊 RÉSUMÉ GLOBAL:\n";
        echo "   👥 Utilisateurs testés: $totalUsers\n";
        echo "   🔐 Connexions réussies: $successfulLogins/$totalUsers\n";
        echo "   📡 Endpoints testés: $totalEndpoints\n";
        echo "   ✅ Endpoints réussis: $successfulEndpoints/$totalEndpoints\n";
        
        $loginRate = $totalUsers > 0 ? round(($successfulLogins / $totalUsers) * 100, 1) : 0;
        $endpointRate = $totalEndpoints > 0 ? round(($successfulEndpoints / $totalEndpoints) * 100, 1) : 0;
        
        echo "   📈 Taux de succès connexions: {$loginRate}%\n";
        echo "   📈 Taux de succès endpoints: {$endpointRate}%\n";
        
        if ($loginRate >= 90 && $endpointRate >= 90) {
            echo "\n🎉 EXCELLENT! La communication Android-API fonctionne parfaitement.\n";
        } elseif ($loginRate >= 70 && $endpointRate >= 70) {
            echo "\n⚠️  BON! Quelques problèmes mineurs détectés.\n";
        } else {
            echo "\n🚨 ATTENTION! Des problèmes significatifs détectés.\n";
        }
        
        echo "\n💡 Recommandations:\n";
        if ($loginRate < 100) {
            echo "   - Vérifier les comptes utilisateurs de test\n";
        }
        if ($endpointRate < 100) {
            echo "   - Vérifier les permissions des rôles\n";
            echo "   - Contrôler la configuration API\n";
        }
        
        echo str_repeat('=', 80) . "\n";
    }
}

// Exécution des tests
echo "🚀 Démarrage des tests de communication Android-API PTMS\n";
echo "🕐 " . date('Y-m-d H:i:s') . "\n";
echo str_repeat('=', 80) . "\n";

$tester = new ApiTester($BASE_URL);
$allResults = [];

foreach ($TEST_USERS as $user) {
    echo "\n" . str_repeat('=', 80) . "\n";
    echo "🧪 TEST UTILISATEUR: {$user['email']} (Rôle: {$user['role_name']})\n";
    echo str_repeat('=', 80) . "\n";
    
    // Test de connexion
    $loginResult = $tester->testLogin($user['email'], $user['password'], $user['expected_role']);
    
    $userResults = [
        'role_name' => $user['role_name'],
        'login' => $loginResult
    ];
    
    // Si connexion réussie, tester les endpoints
    if ($loginResult['success'] && isset($loginResult['token'])) {
        $endpointResults = $tester->testUserEndpoints($user, $loginResult['token']);
        $userResults['endpoints'] = $endpointResults;
    }
    
    $allResults[$user['email']] = $userResults;
    
    // Pause entre les utilisateurs
    sleep(1);
}

// Génération du rapport final
$tester->generateReport($allResults);

echo "\n🏁 Tests terminés à " . date('Y-m-d H:i:s') . "\n";
?>
