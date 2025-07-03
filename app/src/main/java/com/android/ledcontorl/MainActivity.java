package com.android.ledcontorl;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.android.ledcontorl.Utils.SystemPropertiesUtils;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;

/**
 *
 * 操作LED节点实现其功能
 * 注意读写文件节点以及persist属性 需要配置相关权限 . 部分功能需要在framework内实现.
 * 2025年5月29日 D.K Mo
 *
 */
/*
  测试github提交 2025年7月4日02:07:07
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "ledeffect";
    private Switch led_swtich, donotdisturb, accessibilty; // 基础开关
    private Switch discord, twitter, telegram, message; // 应用开关
    private Button btnSlow, btnMedium, btnFast; // 速度按钮
    private Button btnBreath, btnBlink, btnWave, btnRainbow; // 效果按钮
    private SeekBar glowSeekBar; // 发光强度滑块
    private TextView glowInfo, glow_display; // 添加 glow_info TextView 引用

    // 当前选中的速度和效果
    private String currentSpeed = "Medium";
    private String currentEffect = "Breath";
    private int currentGlowLevel = 4; // 默认seekbar位置 中 4

    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable updateGlowRunnable;
    private static final int UPDATE_DELAY = 16; // 约60fps的更新频率
    private boolean isUpdating = false;

    // 添加强度级别常量
    private static final int LEVEL_LOW_MAX = 0;
    private static final int LEVEL_MEDIUM_MAX = 1;
    private static final int LEVEL_HIGH_MAX = 2;

    /**
     * LED 节点以及说明
     * sys/devices/platform/11004000.i2c7/i2c-7/7-006a/leds/aw22xxx_led # ls
     * brightness cfg device effect fw hwen imax max_brightness power reg rgb
     * subsystem task0 task1 trigger uevent
     *
     *
     * 1.加载固件1对应如下灯效
     * echo 1 > fw
     *
     * cfg[0] = aw22xxx_cfg_led_off.bin
     * cfg[1] = aw22xxx_cfg_led_on.bin
     * cfg[2] = aw22xxx_cfg_led_breath.bin
     * cfg[3] = aw22xxx_cfg_led_collision.bin
     * cfg[4] = aw22xxx_cfg_led_skyline.bin
     * cfg[5] = aw22xxx_cfg_led_flower.bin
     * cfg[6] = aw22xxx_cfg_audio_skyline.bin
     * cfg[7] = aw22xxx_cfg_audio_flower.bin
     *
     * 2.加载固件2对应如下灯效
     * echo2 > fw
     * cfg[8] = lamps_init.bin
     * cfg[9] = lamp1_dyna_blink_slow.bin
     * cfg[a] = lamp1_dyna_blink_mid.bin
     * cfg[b] = lamp1_dyna_blink_fast.bin
     * cfg[c] = lamp1_dyna_breath_slow.bin
     * cfg[d] = lamp1_dyna_breath_mid.bin
     * cfg[e] = lamp1_dyna_breath_fast.bin
     * cfg[f] = lamp1_dyna_double_blink_slow.bin
     * cfg[10] = lamp1_dyna_double_blink_mid.bin
     * cfg[11] = lamp1_dyna_double_blink_fast.bin
     * current cfg = lamp1_dyna_blink_slow.bin
     *
     * cat imax
     * imax[0] = AW22XXX_IMAX_2mA
     * imax[1] = AW22XXX_IMAX_3mA
     * imax[2] = AW22XXX_IMAX_4mA
     * imax[3] = AW22XXX_IMAX_6mA
     * imax[4] = AW22XXX_IMAX_9mA
     * imax[5] = AW22XXX_IMAX_10mA
     * imax[6] = AW22XXX_IMAX_15mA
     * imax[7] = AW22XXX_IMAX_20mA
     * imax[8] = AW22XXX_IMAX_30mA
     * imax[9] = AW22XXX_IMAX_40mA
     * imax[a] = AW22XXX_IMAX_45mA
     * imax[b] = AW22XXX_IMAX_60mA
     * imax[c] = AW22XXX_IMAX_75mA
     * current id = 0x01, imax = AW22XXX_IMAX_3mA
     */

    // LED灯效节点

    // led_fw 不同的固件 ,echo 1>fw 固件1 ，echo 2>fw 固件2
    final String led_fw = "sys/devices/platform/11004000.i2c7/i2c-7/7-006a/leds/aw22xxx_led/fw";
    // led_max_brightness 亮度值,根据电流匹配
    /**
     * echo 0~12(0~c) >imax
     */
    final String led_imax = "sys/devices/platform/11004000.i2c7/i2c-7/7-006a/leds/aw22xxx_led/imax";
    /**
     * echo 8 >led_effect 1、先初始化
     * echo 0~c > imax 2、设亮度
     * echo 1 >led_cfg 3、矢能
     * echo 9~11 >led_effect 4、效果
     * echo 1 >led_cfg 5、矢能
     */
    final String led_effect = "sys/devices/platform/11004000.i2c7/i2c-7/7-006a/leds/aw22xxx_led/effect";

    // final String led_cfg =
    // "sys/devices/platform/11004000.i2c7/i2c-7/7-006a/leds/aw22xxx_led/cfg";

    final String led_light = "sys/devices/platform/11004000.i2c7/i2c-7/7-006a/leds/aw22xxx_led/flashlight_blink_led";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // 初始化所有控件
        initializeViews();
        // 设置监听器
        setupListeners();

        // 在UI初始化完成后恢复LED状态
        handler.postDelayed(this::restoreLEDState, 500); // 延迟500ms确保UI完全初始化

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

    }

    private void initializeViews() {
        // 初始化基础开关
        led_swtich = findViewById(R.id.led_swtich); // LED 开关
        donotdisturb = findViewById(R.id.donotdisturb); // Do not disturb
        accessibilty = findViewById(R.id.accessibilty); // Accessibilty

        // 初始化应用开关
        discord = findViewById(R.id.discord);
        twitter = findViewById(R.id.twitter);
        telegram = findViewById(R.id.telegram);
        message = findViewById(R.id.message);

        // 初始化速度按钮
        btnSlow = findViewById(R.id.btn_slow);
        btnMedium = findViewById(R.id.btn_medium);
        btnFast = findViewById(R.id.btn_fast);

        // 初始化效果按钮
        btnBreath = findViewById(R.id.btn_breath);
        btnBlink = findViewById(R.id.btn_blink);
        btnWave = findViewById(R.id.btn_wave);
        btnRainbow = findViewById(R.id.btn_rainbow);

        // 初始化发光强度滑块
        glowSeekBar = findViewById(R.id.glow_seekbar);

        // 初始化发光强度信息显示
        glowInfo = findViewById(R.id.glow_info);
        glow_display = findViewById(R.id.glow_display);

        // 默认状态为LED关闭,各种灯效控件置灰状态不可点击
        discord.setEnabled(false);
        twitter.setEnabled(false);
        telegram.setEnabled(false);
        message.setEnabled(false);
        btnSlow.setEnabled(false);
        btnMedium.setEnabled(false);
        btnFast.setEnabled(false);
        btnBreath.setEnabled(false);
        btnWave.setEnabled(false);
        btnRainbow.setEnabled(false);
        btnBlink.setEnabled(false);
        glowSeekBar.setEnabled(false);
    }

    private void setupListeners() {
        // 基础开关监听器
        led_swtich.setOnCheckedChangeListener((buttonView, isChecked) -> {
            try {
                // 保存LED状态
                String stateValue = isChecked ? "1" : "0";
                SystemPropertiesUtils.set("persist.sys.yrct.led", stateValue);

                // 验证状态是否保存成功
                String savedState = SystemPropertiesUtils.get("persist.sys.yrct.led", "0");
                Log.d(TAG, "LED switch state changed to: " + isChecked +
                        ", saved value: " + savedState +
                        ", verification: " + (stateValue.equals(savedState) ? "success" : "failed"));

                updateLEDStatus(isChecked);
            } catch (Exception e) {
                Log.e(TAG, "Error saving LED state: " + e.getMessage());
                showToast("保存LED状态失败");
            }
        });

        donotdisturb.setOnCheckedChangeListener((buttonView, isChecked) -> {
            updateDNDStatus(isChecked);
        });

        accessibilty.setOnCheckedChangeListener((buttonView, isChecked) -> {
            updateAccessibilityStatus(isChecked);
        });

        // 应用开关监听器
        discord.setOnCheckedChangeListener((buttonView, isChecked) -> {
            updateDiscordStatus(isChecked);
        });

        twitter.setOnCheckedChangeListener((buttonView, isChecked) -> {
            updateTwitterStatus(isChecked);
        });

        telegram.setOnCheckedChangeListener((buttonView, isChecked) -> {
            updateTelegramStatus(isChecked);
        });

        message.setOnCheckedChangeListener((buttonView, isChecked) -> {
            updateMessagingStatus(isChecked);
        });

        // 速度按钮监听器
        btnSlow.setOnClickListener(v -> {
            String speedValue = updateSpeed("Slow");
            // 如果当前有效果，更新效果
            if (currentEffect != null) {
                updateEffect(currentEffect);
            }
        });

        btnMedium.setOnClickListener(v -> {
            String speedValue = updateSpeed("Normal");
            // 如果当前有效果，更新效果
            if (currentEffect != null) {
                updateEffect(currentEffect);
            }
        });

        btnFast.setOnClickListener(v -> {
            String speedValue = updateSpeed("Fast");
            // 如果当前有效果，更新效果
            if (currentEffect != null) {
                updateEffect(currentEffect);
            }
        });

        // 效果按钮监听器
        btnBreath.setOnClickListener(v -> updateEffect("Pulse"));
        btnBlink.setOnClickListener(v -> updateEffect("Blink"));
        btnWave.setOnClickListener(v -> updateEffect("Steady glow"));
        btnRainbow.setOnClickListener(v -> updateEffect("Double blink"));

        // 修改发光强度滑块监听器
        glowSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    // 取消之前的更新
                    if (updateGlowRunnable != null) {
                        handler.removeCallbacks(updateGlowRunnable);
                    }

                    // 创建新的更新任务
                    updateGlowRunnable = () -> {
                        if (!isUpdating) {
                            isUpdating = true;
                            String glowValue = updateGlowLevel(progress);
                            updateGlowInfo(progress); // 更新显示信息
                            // 如果当前有效果，更新效果
                            if (currentEffect != null) {
                                updateEffect(currentEffect);
                            }
                            isUpdating = false;
                        }
                    };

                    // 延迟执行更新
                    handler.postDelayed(updateGlowRunnable, UPDATE_DELAY);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // 开始拖动时，取消所有待处理的更新
                if (updateGlowRunnable != null) {
                    handler.removeCallbacks(updateGlowRunnable);
                }
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // 停止拖动时，立即执行一次更新
                if (updateGlowRunnable != null) {
                    handler.removeCallbacks(updateGlowRunnable);
                }
                int finalProgress = seekBar.getProgress();
                String glowValue = updateGlowLevel(finalProgress);
                updateGlowInfo(finalProgress); // 更新显示信息
                // 如果当前有效果，更新效果
                if (currentEffect != null) {
                    updateEffect(currentEffect);
                }
                String intensityLevel = getIntensityLevel(finalProgress);
                // showToast("发光强度: " + intensityLevel + " (" + glowValue + ")");
            }
        });
    }

    // 修改恢复LED状态的方法
    private void restoreLEDState() {
        try {
            String savedState = SystemPropertiesUtils.get("persist.sys.yrct.led", "0");
            boolean isEnabled = "1".equals(savedState);

            Log.d(TAG, "restoreLEDState: Reading saved state = " + savedState);

            // 先更新UI状态
            led_swtich.setChecked(isEnabled);

            // 然后更新LED状态
            if (isEnabled) {
                Led_on();
                Log.d(TAG, "restoreLEDState: LED turned ON");
            } else {
                Led_off();
                Log.d(TAG, "restoreLEDState: LED turned OFF");
            }
        } catch (Exception e) {
            Log.e(TAG, "restoreLEDState: Error restoring LED state: " + e.getMessage());
            // 发生错误时默认关闭LED
            led_swtich.setChecked(false);
            Led_off();
        }
    }

    private void updateLEDStatus(boolean isEnabled) {
        try {
            if (isEnabled) {
                Led_on();
                // 再次确认状态保存
                SystemPropertiesUtils.set("persist.sys.yrct.led", "1");
                String savedState = SystemPropertiesUtils.get("persist.sys.yrct.led", "0");
                Log.d(TAG, "updateLEDStatus: LED ON, saved state = " + savedState);
            } else {
                Led_off();
                // 再次确认状态保存
                SystemPropertiesUtils.set("persist.sys.yrct.led", "0");
                String savedState = SystemPropertiesUtils.get("persist.sys.yrct.led", "0");
                Log.d(TAG, "updateLEDStatus: LED OFF, saved state = " + savedState);
            }
            showToast("LED " + (isEnabled ? "开启" : "关闭"));
        } catch (Exception e) {
            Log.e(TAG, "updateLEDStatus: Error updating LED status: " + e.getMessage());
            showToast("更新LED状态失败");
        }
    }

    private void updateDNDStatus(boolean isEnabled) {
        // TODO: 实现勿扰模式控制逻辑
        if (isEnabled) {
            SystemPropertiesUtils.set("persist.sys.led.switch", "true"); // 需要配置相关权限 ,userdebug调试时先行关闭selinux
                                                                         // 逻辑功能调通后再来配置权限

        } else {
            SystemPropertiesUtils.set("persist.sys.led.switch", "false");
        }
        showToast("勿扰模式 " + (isEnabled ? "开启" : "关闭"));
    }

    private void updateAccessibilityStatus(boolean isEnabled) {
        // TODO: 实现无障碍服务控制逻辑
        if (isEnabled) {
            if (SystemPropertiesUtils.getBoolean("persist.sys.led.switch", true)) {
                Log.d(TAG, "updateAccessibilityStatus: set persist.sys.led.switch true");
            } else {
                Log.d(TAG, "updateAccessibilityStatus: set persist.sys.led.switch false");
            }
        }
        showToast("无障碍服务 " + (isEnabled ? "开启" : "关闭"));
    }

    // 应用功能实现
    private void updateDiscordStatus(boolean isEnabled) {
        // TODO: 实现 Discord 通知控制逻辑
        showToast("Discord 通知 " + (isEnabled ? "开启" : "关闭"));
    }

    private void updateTwitterStatus(boolean isEnabled) {
        // TODO: 实现 Twitter 通知控制逻辑
        showToast("Twitter 通知 " + (isEnabled ? "开启" : "关闭"));
    }

    private void updateTelegramStatus(boolean isEnabled) {
        // TODO: 实现 Telegram 通知控制逻辑
        showToast("Telegram 通知 " + (isEnabled ? "开启" : "关闭"));
    }

    private void updateMessagingStatus(boolean isEnabled) {
        // TODO: 实现消息通知控制逻辑
        showToast("消息通知 " + (isEnabled ? "开启" : "关闭"));
    }

    // 速度和效果控制
    private String updateSpeed(String speed) {
        currentSpeed = speed;
        String speedValue = "";

        // 重置所有速度按钮状态
        btnSlow.setSelected(false);
        btnMedium.setSelected(false);
        btnFast.setSelected(false);

        // 获取当前效果
        String effect = currentEffect;
        Log.d(TAG, "当前效果是: " + effect);

        // 你可以根据 effect 做不同处理
        //pulse 脉动 ,blink 呼吸 ,double blink 双闪,steady glow 稳定发光
        if ("Pulse".equals(effect)) {
            // do something
            // 根据速度设置对应的值和按钮状态
            switch (speed) {
                case "Slow":
                    speedValue = "9"; // lamp1_dyna_blink_slow.bin
                    btnSlow.setSelected(true);
                    // writeNodeValue(led_imax, "3");
                    break;
                case "Normal":
                    speedValue = "10"; // lamp1_dyna_blink_mid.bin
                    btnMedium.setSelected(true);
                    // writeNodeValue(led_imax, "9");
                    break;
                case "Fast":
                    speedValue = "11"; // lamp1_dyna_blink_fast.bin
                    btnFast.setSelected(true);
                    // writeNodeValue(led_imax, "20");
                    break;
                default:
                    speedValue = "10"; // 默认使用中速
                    btnMedium.setSelected(true);
                    // writeNodeValue(led_imax, "9");
                    break;
            }
        } else if ("Blink".equals(effect)) {
            // do something else
            // 根据速度设置对应的值和按钮状态
            switch (speed) {
                case "Slow":
                    speedValue = "12"; // lamp1_dyna_blink_slow.bin
                    btnSlow.setSelected(true);
                    // writeNodeValue(led_imax, "3");
                    break;
                case "Normal":
                    speedValue = "13"; // lamp1_dyna_blink_mid.bin
                    btnMedium.setSelected(true);
                    // writeNodeValue(led_imax, "9");
                    break;
                case "Fast":
                    speedValue = "14"; // lamp1_dyna_blink_fast.bin
                    btnFast.setSelected(true);
                    // writeNodeValue(led_imax, "20");
                    break;
                default:
                    speedValue = "13"; // 默认使用中速
                    btnMedium.setSelected(true);
                    // writeNodeValue(led_imax, "9");
                    break;
            }
        }else if ("Double blink".equals(effect)) {
            // 根据速度设置对应的值和按钮状态
            switch (speed) {
                case "Slow":
                    speedValue = "20"; // lamp1_dyna_blink_slow.bin
                    btnSlow.setSelected(true);
                    // writeNodeValue(led_imax, "3");
                    break;
                case "Normal":
                    speedValue = "21"; // lamp1_dyna_blink_mid.bin
                    btnMedium.setSelected(true);
                    // writeNodeValue(led_imax, "9");
                    break;
                case "Fast":
                    speedValue = "22"; // lamp1_dyna_blink_fast.bin
                    btnFast.setSelected(true);
                    // writeNodeValue(led_imax, "20");
                    break;
                default:
                    speedValue = "21"; // 默认使用中速
                    btnMedium.setSelected(true);
                    // writeNodeValue(led_imax, "9");
                    break;
            }

        }
        

        Log.d(TAG, "updateSpeed: Setting speed to " + speed + " with value " + speedValue);
        // showToast("速度设置为: " + speed);

        return speedValue;
    }

    private void updateEffect(String effect) {
        currentEffect = effect;

        // 1. 先高亮效果按钮
        btnBreath.setSelected(false);
        btnBlink.setSelected(false);
        btnWave.setSelected(false);
        btnRainbow.setSelected(false);

        if (effect.equals("Pulse")) {
            btnBreath.setSelected(true);
        } else if (effect.equals("Blink")) {
            btnBlink.setSelected(true);
        } else if (effect.equals("Steady glow")) {
            btnWave.setSelected(true);
        } else if (effect.equals("Double blink")) {
            btnRainbow.setSelected(true);
        }

        // 2. 再高亮速度按钮
        // 这里不要调用 updateSpeed，否则会递归
        btnSlow.setSelected(false);
        btnMedium.setSelected(false);
        btnFast.setSelected(false);

        switch (currentSpeed) {
            case "Slow":
                btnSlow.setSelected(true);
                break;
            case "Normal":
                btnMedium.setSelected(true);
                break;
            case "Fast":
                btnFast.setSelected(true);
                break;
            default:
                btnMedium.setSelected(true);
                break;
        }

        // 3. 获取当前亮度
        String glowValue = updateGlowLevel(currentGlowLevel);

        // 4. 写节点
        if (effect.equals("Blink")) {
            writeNodeValue(led_effect, "9");
            writeNodeValue(led_imax, glowValue);
            writeNodeValue(led_light, "1");
        } else if (effect.equals("Pulse")) {
            writeNodeValue(led_effect, "12");
            writeNodeValue(led_imax, glowValue);
            writeNodeValue(led_light, "1");
        } else if (effect.equals("Steady glow")) {
            writeNodeValue(led_effect, "19");
            writeNodeValue(led_imax, glowValue);
            writeNodeValue(led_light, "1");
        } else if (effect.equals("Double blink")) {
            writeNodeValue(led_effect, "15");
            writeNodeValue(led_imax, glowValue);
            writeNodeValue(led_light, "1");
        }

        showToast("效果设置为: " + effect + " 速度设置为: " + currentSpeed + " 发光强度: " + glowValue);
    }

    private String getIntensityLevel(int progress) {
        if (progress <= LEVEL_LOW_MAX) {
            return "1"; // general -> 0
        } else if (progress <= LEVEL_MEDIUM_MAX) {
            return "4"; // medium -> 1
        } else if (progress <= LEVEL_HIGH_MAX) {
            return "7"; // strong -> 2
        } else {
            return "4"; // medium -> 1
        }
    }

    private String updateGlowLevel(int level) {
        if (level != currentGlowLevel) {
            currentGlowLevel = level;
            String intensityValue = getIntensityLevel(level);

            Log.d(TAG, "updateGlowLevel: XX intensityValue = " + intensityValue);
            String intensityLevel;

            // 根据数值确定显示文本
            if (intensityValue.equals("6")) {
                intensityLevel = "general";
            } else if (intensityValue.equals("8")) {
                intensityLevel = "muduim";
            } else if (intensityValue.equals("10")) {
                intensityLevel = "strong";
            } else {
                intensityLevel = "muduim";
            }

            Log.d(TAG, "updateGlowLevel: intensityLevel=" + intensityLevel + ", value=" + intensityValue);
            glow_display.setText(intensityLevel);
        }
        return getIntensityLevel(currentGlowLevel); // 返回映射后的强度值 (6, 8, 10)
    }

    private void updateGlowInfo(int progress) {
        String intensityValue = getIntensityLevel(progress);
        String intensityLevel;

        // 根据数值确定显示文本
        if (intensityValue.equals("6")) {
            intensityLevel = "general";
        } else if (intensityValue.equals("8")) {
            intensityLevel = "medium";
        } else if (intensityValue.equals("10")) {
            intensityLevel = "strong";
        } else {
            intensityLevel = "medium";
        }

        String infoText = String.format("发光强度: %s (%s)", intensityLevel, intensityValue);
        glowInfo.setText(infoText);
    }

    // 工具方法
    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    /**
     * 写入节点值
     *
     * @param fileName 节点路径
     * @param value    要写入的整型值
     */

    public void writeNodeValue(String fileName, String value) {
        Log.d(TAG, "writeNodeValue: fileName =" + fileName + " value=" + value);

        try {
            FileWriter fileWriter = new FileWriter(fileName);
            fileWriter.write(value);

            Log.d(TAG, "writeNodeValue: fileName =" + fileName + "value =" + value);
            fileWriter.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 当LED灯关闭时,清空所有效果并置灰
     */
    private void Led_off() {
        try {
            SystemPropertiesUtils.set("persist.sys.yrct.led", "0");
            String currentState = SystemPropertiesUtils.get("persist.sys.yrct.led", "0");
            Log.d(TAG, "Led_off: Current state = " + currentState);

            writeNodeValue(led_fw, "2");
            // 禁用所有控件
            discord.setEnabled(false);
            twitter.setEnabled(false);
            telegram.setEnabled(false);
            message.setEnabled(false);
            btnSlow.setEnabled(false);
            btnMedium.setEnabled(false);
            btnFast.setEnabled(false);
            btnBreath.setEnabled(false);
            btnWave.setEnabled(false);
            btnRainbow.setEnabled(false);
            btnBlink.setEnabled(false);
            glowSeekBar.setEnabled(false);
        } catch (Exception e) {
            Log.e(TAG, "Led_off: Error: " + e.getMessage());
        }
    }

    /**
     * 当LED 打开时 各灯效控件可点击
     */
    private void Led_on() {
        try {
            SystemPropertiesUtils.set("persist.sys.yrct.led", "1");
            String currentState = SystemPropertiesUtils.get("persist.sys.yrct.led", "0");
            Log.d(TAG, "Led_on: Current state = " + currentState);

            // writeNodeValue(led_fw,"1");
            // 启用所有控件
            discord.setEnabled(false);
            twitter.setEnabled(false);
            telegram.setEnabled(false);
            message.setEnabled(false);
            btnSlow.setEnabled(true);
            btnMedium.setEnabled(true);
            btnFast.setEnabled(true);
            btnBreath.setEnabled(true);
            btnWave.setEnabled(true);
            btnRainbow.setEnabled(true);
            btnBlink.setEnabled(true);
            glowSeekBar.setEnabled(true);
        } catch (Exception e) {
            Log.e(TAG, "Led_on: Error: " + e.getMessage());
        }
    }

}