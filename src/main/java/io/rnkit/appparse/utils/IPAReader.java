package io.rnkit.appparse.utils;

import java.io.*;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.parsers.ParserConfigurationException;

import io.rnkit.appparse.entity.IPAInfo;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.xml.sax.SAXException;

import com.dd.plist.NSArray;
import com.dd.plist.NSDictionary;
import com.dd.plist.NSObject;
import com.dd.plist.PropertyListFormatException;
import com.dd.plist.PropertyListParser;

/**
 * Name: IPAReader
 * Author: SimMan [liwei0990@gmail.com]
 * CreatedAt: 02/10/2017
 * Description:
 * Copyright (c) 2017 Toutoo, Inc.
 */
public class IPAReader {
    private String fileName;

    private String mApngdefryPath = null;

    public IPAReader(String fileName) {
        this.fileName = fileName;
        String osName = System.getProperty("os.name");
        if (osName.toLowerCase().contains("mac")) {
            mApngdefryPath = this.getClass().getResource("/bin/macosx-pngdefry").getPath();
        } else if (osName.toLowerCase().contains("linux")) {
            mApngdefryPath = this.getClass().getResource("/bin/linux-pngdefry").getPath();
        }
    }

    public String getLastIconFileName(NSDictionary dict, String identifier) {
        String name = null;
        NSObject[] files;

        if (identifier.equals("CFBundleIconFiles")) {
            files = ((NSArray) dict.get("CFBundleIconFiles")).getArray();
        } else {
            NSDictionary primaryIcon = (NSDictionary) dict.get(identifier);
            NSDictionary iconFiles = (NSDictionary) primaryIcon.get("CFBundlePrimaryIcon");
            files = ((NSArray) iconFiles.get("CFBundleIconFiles")).getArray();
        }

        for (NSObject file : files) {
            name = file.toString();
            System.out.println(name);
        }

        return name;
    }

    public IPAInfo parse() throws IOException, PropertyListFormatException, ParseException, ParserConfigurationException, SAXException {
//        if (mAaptPath == null) throw new Exception("不支持的系统!");
        IPAInfo info = new IPAInfo();

        File f = new File(this.fileName);
        info.setFileSize(f.length());

        ZipInputStream zipIn = new ZipInputStream(new FileInputStream(this.fileName));
        ZipEntry entry = zipIn.getNextEntry();

        while (entry != null) {
            if (entry.getName().endsWith(".app/embedded.mobileprovision")) {
                ByteArrayOutputStream stream = readFileToMemory(zipIn);
                info.setMobileProvisionFile(stream.toByteArray());
                String plist = getPlistFromMobileProvisionFile(stream);

                if (plist != null) {
                    NSDictionary rootDict = (NSDictionary) PropertyListParser.parse(plist.getBytes());
                    info.setProvisioningProfileCreationDate(rootDict.get("CreationDate").toString());
                    info.setProvisioningProfileExpirationDate(rootDict.get("ExpirationDate").toString());
                    info.setProvisioningProfileName(rootDict.get("Name").toString());
                    info.setTeamIdentifier(rootDict.get("TeamIdentifier").toString());
                    info.setTeamName(rootDict.get("TeamName").toString());

                    if (rootDict.get("ProvisionsAllDevices") != null) {
                        info.setEnv(1);
                    }

                    if (rootDict.get("ProvisionedDevices") != null) {
                        NSObject[] devices = ((NSArray) rootDict.get("ProvisionedDevices")).getArray();

                        List<String> list = new ArrayList<String>();
                        for (NSObject device : devices) {
                            list.add(device.toString());
                        }
                        info.setEnv(2);
                        info.setProvisioningProfileDevices(list);
                    }

                } else {
                    return null;
                }
            } else if (entry.getName().endsWith(".app/Info.plist")) {
                info.setInfoPlistFile(readFileToMemory(zipIn).toByteArray());
                NSDictionary rootDict = (NSDictionary) PropertyListParser.parse(info.getInfoPlistFile());

                info.setMinimumOSVersion(rootDict.get("MinimumOSVersion").toString());
                info.setBundleName(rootDict.get("CFBundleName").toString());
                info.setBundleVersionString(rootDict.get("CFBundleShortVersionString").toString());
                info.setBundleIdentifier(rootDict.get("CFBundleIdentifier").toString());
                info.setBuildNumber(rootDict.get("CFBundleVersion").toString());
                info.setPlatformVersion(rootDict.get("DTPlatformVersion").toString());

                if (rootDict.containsKey("UIRequiredDeviceCapabilities")) {
                    NSObject[] o = ((NSArray) rootDict.get("UIRequiredDeviceCapabilities")).getArray();

                    if (o.length > 0) {
                        info.setRequiredDeviceCapabilities(o[0].toString());
                    }
                }

                if (rootDict.containsKey("CFBundleIcons")) {
                    info.setiPhoneSupport(true);
                    info.setBundleIconFileName(this.getLastIconFileName(rootDict, "CFBundleIcons"));
                } else {
                    info.setiPhoneSupport(false);
                }

                if (rootDict.containsKey("CFBundleIconFiles")) {
                    info.setiPhoneSupport(true);
                    info.setBundleIconFileName(this.getLastIconFileName(rootDict, "CFBundleIconFiles"));
                }

                if (rootDict.containsKey("CFBundleIcons~ipad")) {
                    info.setiPadSupport(true);

                    info.setBundleIconFileName(this.getLastIconFileName(rootDict, "CFBundleIcons~ipad"));
                } else {
                    info.setiPadSupport(false);
                }
            }
            zipIn.closeEntry();
            entry = zipIn.getNextEntry();
        }
        zipIn.close();

        // 此处需要优化, 可以减少2s左右
        if (info.getBundleIconFileName() != null) {
            zipIn = new ZipInputStream(new FileInputStream(this.fileName));
            entry = zipIn.getNextEntry();
            while (entry != null) {
                if (entry.getName().contains(info.getBundleIconFileName().trim())) {
                    // 先写入文件
                    File pngTmpFile = this.writeFileToTmp(zipIn);
                    // 调用 pngdefry 修复图片
                    File pngdefryFile = this.pngdefry(pngTmpFile);
                    // 读取图片
                    FileInputStream fileInputStream = new FileInputStream(pngdefryFile);
                    info.setBundleIcon(IOUtils.toByteArray(fileInputStream));
                    break;
                }
                zipIn.closeEntry();
                entry = zipIn.getNextEntry();
            }
            zipIn.close();
        }
        if (info.getBundleIdentifier() != null) {
            return info;
        }
        return null;
    }

    public String getPlistFromMobileProvisionFile(ByteArrayOutputStream stream) throws UnsupportedEncodingException {
        String s = stream.toString("UTF-8");
        int i = s.indexOf("<plist version=\"1.0\">");
        if (i >= 0) {
            s = s.substring(i);
            i = s.indexOf("</plist>");
            if (i >= 0) {
                String plist = s.substring(0, i + "</plist>".length());
                return plist;
            }
        }
        return null;
    }

    public ByteArrayOutputStream readFileToMemory(ZipInputStream zipIn) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] bytesIn = new byte[4096];
        int read = 0;
        while ((read = zipIn.read(bytesIn)) != -1) {
            bos.write(bytesIn, 0, read);
        }
        bos.close();
        return bos;
    }

    private File writeFileToTmp(ZipInputStream zipInputStream) throws IOException {
        String tmpFilePath = String.format("%s/%s.png", System.getProperty("java.io.tmpdir"), UUID.randomUUID().toString());
        File file = new File(tmpFilePath);
        OutputStream output = new FileOutputStream(file);
        BufferedOutputStream bufferedOutput = new BufferedOutputStream(output);
        bufferedOutput.write(readFileToMemory(zipInputStream).toByteArray());
        return file;
    }

    private File pngdefry(File pngFile) throws IOException {
        ProcessBuilder mBuilder = new ProcessBuilder();
        mBuilder.redirectErrorStream(true);
        ProcessBuilder processBuilder = mBuilder.command(mApngdefryPath, "-s", "-pngdefry", "-o", pngFile.getParent(), pngFile.getAbsolutePath());
        Process process = processBuilder.start();
        InputStream is = process.getInputStream();
        BufferedReader br = new BufferedReader(
                new InputStreamReader(is, "utf8"));
        String tmp = br.readLine();
        System.out.println(tmp);
        File pngdefryFile = new File(String.format("%s/%s-pngdefry.png", pngFile.getParent(), FilenameUtils.getBaseName(pngFile.getName())));
        System.out.println(pngdefryFile.getPath());
        return pngdefryFile;
    }
}