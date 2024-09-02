package com.yecp.ringtonesettings;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.Settings;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

import pub.devrel.easypermissions.EasyPermissions;

public class RingtoneActivity extends Activity implements EasyPermissions.PermissionCallbacks{

    public enum PermissionStatus {
        INIT,
        NONE,
        HAVE,
    }

    private TextView tvGetPermission;
    private TextView tvCurrentRingtoneName;

    private PermissionStatus nPermissionStatus = PermissionStatus.INIT;

    private static final int REQUEST_CODE_SELECT_RINGTONE = 1;
    private static final int REQUEST_CODE_WRITE_SETTINGS = 2;
    private static final int REQUEST_CODE_READ_EXTERNAL_STORAGE = 3;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ringtone);

        /// btn
        Button mButtonAlarm = findViewById(R.id.buttonAlarm);
        /// get permission txt
        tvGetPermission = findViewById(R.id.tvGetPermission);
        ///
        tvCurrentRingtoneName = findViewById(R.id.tvCurrentRingtoneName);

        mButtonAlarm.setOnClickListener(v -> {
            if (!Settings.System.canWrite(RingtoneActivity.this)) {
                Toast.makeText(RingtoneActivity.this,
                        getString(R.string.give_change_system_setttings_permission_first),
                        Toast.LENGTH_SHORT).show();
                return;
            }

            // 打开系统铃声设置
            Intent intent = new Intent(
                    RingtoneManager.ACTION_RINGTONE_PICKER);
            Uri currentTone = RingtoneManager.getActualDefaultRingtoneUri(RingtoneActivity.this,
                    RingtoneManager.TYPE_ALARM);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, currentTone);
            // 设置铃声类型和title
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE,
                    RingtoneManager.TYPE_ALARM);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE,
                    getString(R.string.select_ringtone));

            // 当设置完毕之后返回到当前的Activity
            startActivityForResult(intent, REQUEST_CODE_SELECT_RINGTONE);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        updatePermissionStatus();
        updateCurrentRingtone();
    }

    /** 刷新当前闹钟铃声 **/
    private void updateCurrentRingtone() {
        Uri uri = RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM);
        Log.d("MyLog", "uri: " + uri.toString());

        if (Utils.isExternalResource(uri)) {
            // 外部资源
            if (EasyPermissions.hasPermissions(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                String ringtoneName = Utils.removeFilenameSuffix(Utils.getMediaDataFromURI(this, uri, MediaStore.Audio.Media.DATA));
                Log.d("MyLog", "Ringtone name: " + ringtoneName);
                tvCurrentRingtoneName.setText(ringtoneName);
            } else {
                // 未授权
                updateGetReadStoragePermissionTV(getString(R.string.custom_ringtone));
            }
        } else {
            // 系统资源
            Ringtone ringtone = RingtoneManager.getRingtone(this, uri);
            Log.d("MyLog", "Ringtone title: " + ringtone.getTitle(this));
            tvCurrentRingtoneName.setText(ringtone.getTitle(this));
        }
    }

    /** 刷新 修改系统设置 权限状态 **/
    private void updatePermissionStatus() {
        if (Settings.System.canWrite(this)) {
            if (nPermissionStatus != PermissionStatus.HAVE) {
                updateGetPermissionTextView("当前已获得权限，如有需要，可点击此处手动取消");
                nPermissionStatus = PermissionStatus.HAVE;
            }
        } else {
            if (nPermissionStatus != PermissionStatus.NONE) {
                updateGetPermissionTextView("当前未获得权限，点击此处进行手动授权");
                nPermissionStatus = PermissionStatus.NONE;
            }
        }
    }

    private void updateGetReadStoragePermissionTV(String str) {
        SpannableString spannableString = makeClickableSpan(str, 0, str.length(), this::requiresReadStoragePermission);

        tvCurrentRingtoneName.setText(spannableString);
        tvCurrentRingtoneName.setMovementMethod(LinkMovementMethod.getInstance());
    }

    private void requiresReadStoragePermission() {
        List<String> perms = new ArrayList<>();
        perms.add(Manifest.permission.READ_EXTERNAL_STORAGE);

        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            // 授权失败且不再询问，引导手动授权
            goAppDetailSettings(this);
        } else {
            // 未授权过
            //Toast.makeText(this, "未授权", Toast.LENGTH_SHORT).show();
            EasyPermissions.requestPermissions(this, getString(R.string.ringtone_name_must_get_permission),
                    REQUEST_CODE_READ_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE);
        }
    }

    public static void goAppDetailSettings(Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage(context.getString(R.string.ringtone_name_must_get_permission_manual_authorization));
        builder.setPositiveButton("确定", (dialog, which) -> {
            dialog.dismiss();

            // 跳转到应用详细设置界面
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setData(Uri.parse("package:" + context.getPackageName()));
            context.startActivity(intent);
        });
        builder.setNegativeButton("取消", (dialog, which) -> dialog.dismiss());

        // 创建并显示对话框
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /** 刷新 修改系统设置 权限状态的文本，重新设置ClickableSpan **/
    private void updateGetPermissionTextView(String str) {
        int start = str.indexOf("点击此处");
        int end = start + 4;

        if (start == -1) {
            return;
        }

        SpannableString spannableString = makeClickableSpan(str, start, end, () -> {
            // 引导用户到设置页面手动授予权限
            Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, REQUEST_CODE_WRITE_SETTINGS);
        });

        tvGetPermission.setText(spannableString);
        tvGetPermission.setMovementMethod(LinkMovementMethod.getInstance());
    }

    public interface OnTextClickListener {
        void onTextClick();
    }

    // 更新后的辅助方法来创建 SpannableString
    private SpannableString makeClickableSpan(String str, int start, int end, OnTextClickListener listener) {
        SpannableString spannableString = new SpannableString(str);

        // 创建一个 ClickableSpan 并重写 onClick 方法
        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(View widget) {
                ((TextView)widget).setHighlightColor(Color.TRANSPARENT);
                // 使用传入的自定义接口调用其 onTextClick 方法
                if (listener != null) {
                    listener.onTextClick();
                }
            }

            @Override
            public void updateDrawState(TextPaint ds) {
                super.updateDrawState(ds);
                ds.setUnderlineText(false); // 去掉点击文字的下划线（可选）
            }
        };

        // 设置 SpannableString 中指定位置的 ClickableSpan
        spannableString.setSpan(clickableSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        return spannableString;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode)
        {
            case REQUEST_CODE_WRITE_SETTINGS:
                // 请求获得 修改系统设置 权限的回调
                // 此处不需要处理，OnResume中会进行更新
                break;
            case REQUEST_CODE_SELECT_RINGTONE:
                // 选择铃声的回调
                if (resultCode != RESULT_OK) {
                    break;
                }
                if (!Settings.System.canWrite(RingtoneActivity.this)) {
                    Toast.makeText(RingtoneActivity.this,
                            getString(R.string.give_change_system_setttings_permission_first),
                            Toast.LENGTH_SHORT).show();
                    break;
                }
                Uri pickedUri = data
                        .getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
                RingtoneManager.setActualDefaultRingtoneUri(this,
                        RingtoneManager.TYPE_ALARM, pickedUri);
                break;
            default: break;
        }
    }

    @Override
    public void onPermissionsGranted(int requestCode, @NonNull List<String> perms) {
        //Toast.makeText(this, "授权成功返回", Toast.LENGTH_SHORT).show();
        if (requestCode == REQUEST_CODE_READ_EXTERNAL_STORAGE) {
            if (perms.contains(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                updateCurrentRingtone();
            }
        }
    }

    @Override
    public void onPermissionsDenied(int requestCode, @NonNull List<String> perms) {
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // Forward results to EasyPermissions
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }
}