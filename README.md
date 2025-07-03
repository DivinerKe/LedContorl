# LedContorl
读取节点控制LED灯效 . 内置代码时需要给节点加相应权限
sys/devices/platform/11004000.i2c7/i2c-7/7-006a/leds/aw22xxx_led/fw
sys/devices/platform/11004000.i2c7/i2c-7/7-006a/leds/aw22xxx_led/imax
sys/devices/platform/11004000.i2c7/i2c-7/7-006a/leds/aw22xxx_led/effect

    //节点读取
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
