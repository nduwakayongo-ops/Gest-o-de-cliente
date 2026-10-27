package com.example.util

import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

object BiometricHelper {

    fun findActivity(context: Context): FragmentActivity? {
        var currentContext: Context? = context
        while (currentContext is ContextWrapper) {
            if (currentContext is FragmentActivity) {
                return currentContext
            }
            currentContext = currentContext.baseContext
        }
        return null
    }

    fun isBiometricAvailable(context: Context): Boolean {
        val biometricManager = BiometricManager.from(context)
        val authenticators = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        } else {
            BiometricManager.Authenticators.BIOMETRIC_STRONG
        }
        val result = biometricManager.canAuthenticate(authenticators)
        return result == BiometricManager.BIOMETRIC_SUCCESS
    }

    fun getBiometricStatusDescription(context: Context): String {
        val biometricManager = BiometricManager.from(context)
        val authenticators = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        } else {
            BiometricManager.Authenticators.BIOMETRIC_STRONG
        }
        return when (biometricManager.canAuthenticate(authenticators)) {
            BiometricManager.BIOMETRIC_SUCCESS -> "Sensor biométrico disponível e pronto para uso."
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> "Este dispositivo não possui sensor biométrico (digital/facial)."
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> "O sensor biométrico está temporariamente indisponível."
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> "Nenhuma digital ou rosto cadastrado nas configurações do telefone."
            else -> "Biometria não configurada no telefone."
        }
    }

    fun launchBiometricPrompt(
        context: Context,
        onSuccess: () -> Unit,
        onError: (String) -> Unit = {}
    ) {
        val activity = findActivity(context)
        if (activity == null) {
            onError("Janela do aplicativo não encontrada para autenticação biométrica.")
            return
        }

        val executor = ContextCompat.getMainExecutor(activity)
        val promptInfoBuilder = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Autenticação Biométrica Mbuli-Microcrédito")
            .setSubtitle("Acesso Protegido à Carteira de Crédito")
            .setDescription("Aproxime o dedo no leitor de digital ou olhe para a câmera para desbloquear o sistema.")

        val promptInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            promptInfoBuilder.setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.BIOMETRIC_WEAK or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
            ).build()
        } else {
            promptInfoBuilder
                .setNegativeButtonText("Usar PIN de Acesso")
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK)
                .build()
        }

        val biometricPrompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    Toast.makeText(context, "Autenticação biométrica validada com sucesso!", Toast.LENGTH_SHORT).show()
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    if (errorCode != BiometricPrompt.ERROR_USER_CANCELED && errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                        onError(errString.toString())
                    }
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    onError("Digital ou Rosto não reconhecido. Tente novamente.")
                }
            }
        )

        try {
            biometricPrompt.authenticate(promptInfo)
        } catch (e: Exception) {
            e.printStackTrace()
            onError("Erro ao abrir leitor biométrico: ${e.localizedMessage}")
        }
    }
}

