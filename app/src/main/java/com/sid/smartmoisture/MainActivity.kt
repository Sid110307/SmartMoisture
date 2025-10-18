package com.sid.smartmoisture

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import androidx.activity.viewModels
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.sid.smartmoisture.ui.screens.EquationEditScreen
import com.sid.smartmoisture.ui.screens.EquationListScreen
import com.sid.smartmoisture.ui.screens.HomeScreen
import com.sid.smartmoisture.ui.screens.LogScreen
import com.sid.smartmoisture.ui.screens.ScanScreen
import com.sid.smartmoisture.ui.theme.SmartMoistureTheme
import com.sid.smartmoisture.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels()
    private val permissionLauncher = registerForActivityResult(RequestMultiplePermissions()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        requestBlePerms()
        setContent { SmartMoistureApp(vm) }
    }

    private fun requestBlePerms() {
        val perms = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= 31) {
            perms += Manifest.permission.BLUETOOTH_SCAN
            perms += Manifest.permission.BLUETOOTH_CONNECT
        } else {
            perms += Manifest.permission.ACCESS_FINE_LOCATION
            perms += Manifest.permission.ACCESS_COARSE_LOCATION
        }

        permissionLauncher.launch(perms.toTypedArray())
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartMoistureApp(vm: MainViewModel) {
    SmartMoistureTheme {
        val nav = rememberNavController()
        NavHost(navController = nav, startDestination = "home") {
            composable("home") {
                HomeScreen(
                    vm = vm,
                    onOpenLog = { nav.navigate("log") },
                    onOpenEquations = { nav.navigate("equations") },
                    onOpenScan = { nav.navigate("scan") })
            }
            composable("scan") { ScanScreen(vm = vm, onConnected = { nav.popBackStack() }) }
            composable("log") { LogScreen(vm = vm, onBack = { nav.popBackStack() }) }
            composable("equations") {
                EquationListScreen(
                    vm = vm,
                    onAdd = { nav.navigate("equations/edit") },
                    onEdit = { eq -> nav.navigate("equations/edit/${eq.id}") },
                    onBack = { nav.popBackStack() })
            }
            composable("equations/edit") {
                EquationEditScreen(vm = vm, onClose = { nav.popBackStack() })
            }
            composable(
                route = "equations/edit/{id}",
                arguments = listOf(navArgument("id") { type = NavType.LongType })
            ) { backStackEntry ->
                EquationEditScreen(
                    vm = vm,
                    existing = vm.equations.collectAsState().value.firstOrNull {
                        it.id == (backStackEntry.arguments?.getLong("id") ?: -1L)
                    },
                    onClose = { nav.popBackStack() })
            }
        }
    }
}

@Preview
@Composable
fun PreviewApp() {
    SmartMoistureApp(MainViewModel.preview())
}