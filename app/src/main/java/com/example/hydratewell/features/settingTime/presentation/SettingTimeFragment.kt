package com.example.hydratewell.features.settingTime.presentation

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.hydratewell.MainActivity
import com.example.hydratewell.MainActivity2
import com.example.hydratewell.R
import com.example.hydratewell.databinding.FragmentSettingTimeBinding
import com.google.android.material.snackbar.Snackbar

private val channelId = "my_channel_id"
private val notificationId = 99

class SettingTimeFragment : Fragment(R.layout.fragment_setting_time) {

    private lateinit var binding: FragmentSettingTimeBinding
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {

        } else {
            Snackbar.make(
                binding.root,
                getString(R.string.notifications_are_off),
                Snackbar.LENGTH_LONG
            ).show()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentSettingTimeBinding.bind(view)

        with(binding) {
            btnSave.setOnClickListener {
                when {
                    ContextCompat.checkSelfPermission(
                        requireContext(), Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED -> {
                        // Создаем канал уведомлений (требуется для версий Android 8.0 и выше)
                        createNotificationChannel()

                        // Устанавливаем время для отправки уведомления (здесь устанавливается через 10 секунд после запуска)
                        tilMinutes.editText?.text
                        if (tilMinutes.editText?.text.toString() == "")
                            Snackbar.make(
                                binding.root,
                                getString(R.string.set_time),
                                Snackbar.LENGTH_LONG
                            ).show()
                        else {
                            val sharedPref =
                                activity?.getSharedPreferences("1", Context.MODE_PRIVATE) ?: return@setOnClickListener
                            with(sharedPref.edit()) {
                                putLong("time", tilMinutes.editText?.text.toString()
                                    .toLong() * 60 * 1000)
                                apply()
                            }

                            val notificationTimeMillis =
                                SystemClock.elapsedRealtime() + tilMinutes.editText?.text.toString()
                                    .toLong() * 60 * 1000

                            // Создаем и настраиваем PendingIntent для отправки уведомления
                            val notificationIntent =
                                Intent(requireContext(), NotificationReceiver::class.java)
                            val pendingIntent = PendingIntent.getBroadcast(
                                requireContext(), 0, notificationIntent,
                                PendingIntent.FLAG_IMMUTABLE
                            )

                            // Получаем AlarmManager и устанавливаем будильник для отправки уведомления
                            val alarmManager =
                                requireActivity().getSystemService(Context.ALARM_SERVICE) as AlarmManager
                            alarmManager.setExact(
                                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                                notificationTimeMillis,
                                pendingIntent
                            )

                            Snackbar.make(
                                binding.root,
                                getString(R.string.scheduled),
                                Snackbar.LENGTH_LONG
                            ).show()
                        }
                    }

                    shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                        Snackbar.make(
                            binding.root,
                            getString(R.string.notifications_are_off),
                            Snackbar.LENGTH_LONG
                        ).show()
                    }

                    else -> {
                        // The registered ActivityResultCallback gets the result of this request
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            requestPermissionLauncher.launch(
                                Manifest.permission.POST_NOTIFICATIONS
                            )
                        }
                    }
                }
            }
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            channelId,
            "My Channel",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        val notificationManager =
            requireActivity().getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    companion object {
        const val TAG = "MainActivity"
        const val NOTIFICATION_MESSAGE_TAG = "message from notification"
        fun newIntent(context: Context) = Intent(context, MainActivity2::class.java).apply {
            putExtra(
                NOTIFICATION_MESSAGE_TAG, "1"
            )
        }
    }

}

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context != null) {
            sendNotification(context)

            val sharedPref = context.getSharedPreferences("1", Context.MODE_PRIVATE) ?: return
            val defaultValue = 600L * 1000L
            val time = sharedPref.getLong("time", defaultValue)

            val notificationTimeMillis =
                SystemClock.elapsedRealtime() + time

            // Создаем и настраиваем PendingIntent для отправки уведомления
            val notificationIntent =
                Intent(context, NotificationReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context, 0, notificationIntent,
                PendingIntent.FLAG_IMMUTABLE
            )

            // Получаем AlarmManager и устанавливаем будильник для отправки уведомления
            val alarmManager =
                context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.setExact(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                notificationTimeMillis,
                pendingIntent
            )
        }
    }
}