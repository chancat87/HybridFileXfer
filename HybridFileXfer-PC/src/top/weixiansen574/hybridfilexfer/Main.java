package top.weixiansen574.hybridfilexfer;

import top.weixiansen574.hybridfilexfer.jdkclient.HFXClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class Main {
    public static void main(String[] args) throws IOException {
        Map<String, String> paramMap = new HashMap<>();
        parseArguments(paramMap, args);
        String connect = paramMap.get("-c");
        if (connect == null) {
            /*
            未指定控制通道连接方式
            参数说明：
            -c 控制通道连接方式 "adb" 或 网络ip
            -s adb连接方式下指定的设备（adb有多设备的情况），你可以用"adb devices"命令查看设备
            示例：
            -c adb
            -c adb -s abcd1234
            -c 192.168.1.2
            */
            System.out.println(Strings.get("usage"));
        } else if (connect.equals("adb")) {
            if (executeAdbForwardCommand(5740, paramMap.get("-s"))) {
                System.out.println("USB_ADB : 5740 端口转发成功！");
                HFXClient hfxClient = new HFXClient("127.0.0.1", 5740);
                if (hfxClient.connect()) {
                    hfxClient.start();
                }
            }
        } else {
            HFXClient hfxClient = new HFXClient(connect, 5740);
            if (hfxClient.connect()) {
                hfxClient.start();
            }
        }
    }

    private static void parseArguments(Map<String, String> paramMap, String[] args) {
        // 遍历数组并解析参数
        for (int i = 0; i < args.length; i++) {
            if (args[i].startsWith("-") && i + 1 < args.length && !args[i + 1].startsWith("-")) {
                paramMap.put(args[i], args[i + 1]);
                i++; // 跳过值
            }
        }
    }

    public static boolean executeAdbForwardCommand(int port, String device) {
        try {
            // 获取当前jar包所在的目录
            String jarDirectory = System.getProperty("user.dir");

            // 执行 adb version 命令来检查 adb 是否在环境变量中
            Process process = Runtime.getRuntime().exec("adb version");
            int exitCode = process.waitFor();

            // 如果 adb version 执行成功，则说明 adb 已在环境变量中
            String adbPath;
            if (exitCode == 0) {
                adbPath = "adb"; // 环境变量中找到adb
            } else {
                adbPath = "./adb"; // 如果没有找到adb，使用当前目录下的 adb
            }

            // 构建adb forward命令
            StringBuilder adbCommand = new StringBuilder();
            adbCommand.append(adbPath);

            if (device != null) {
                adbCommand.append(" -s ").append(device);
            }
            adbCommand.append(" forward tcp:");
            adbCommand.append(port);
            adbCommand.append(" tcp:");
            adbCommand.append(port);

            // 执行adb forward命令
            Process forwardProcess = Runtime.getRuntime().exec(adbCommand.toString(), null, new java.io.File(jarDirectory));
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(forwardProcess.getErrorStream()));
            String l;
            while ((l = errorReader.readLine()) != null) {
                System.err.println(l);
            }

            // 读取adb forward命令执行的输出
            BufferedReader reader = new BufferedReader(new InputStreamReader(forwardProcess.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("adb:" + line);
            }

            // 等待命令执行完成
            int forwardExitCode = forwardProcess.waitFor();
            return forwardExitCode == 0;

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return false; // 执行失败
        }
    }

}
