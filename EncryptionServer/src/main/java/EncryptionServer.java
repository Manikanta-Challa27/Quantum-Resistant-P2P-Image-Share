import io.javalin.Javalin;
import io.javalin.http.UploadedFile;
import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;


public class EncryptionServer {

    public static int[] getBestFactorPair(int number) {
        int minDifference = Integer.MAX_VALUE;
        int[] bestPair = new int[2];
        for (int i = 1; i <= Math.sqrt(number); i++) {
            if (number % i == 0) {
                int pair = number / i;
                int difference = pair - i;
                if (difference < minDifference) {
                    minDifference = difference;
                    bestPair[0] = i;
                    bestPair[1] = pair;
                }
            }
        }
        return bestPair;
    }

    public static BufferedImage generateEncryptedImage(List<Integer> pixelValues) {
        int[] dim = getBestFactorPair(pixelValues.size());
        int height = dim[0];
        int width = dim[1];
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int index = y * width + x;
                if (index < pixelValues.size()) {
                    int gray = (pixelValues.get(index) & 0xFF) * 4;
                    int rgb = (gray << 16) | (gray << 8) | gray;
                    image.setRGB(x, y, rgb);
                }
            }
        }
        return image;
    }

    public static List<Integer> convertImageToList(BufferedImage img) {
        int height = img.getHeight();
        int width = img.getWidth();
        List<Integer> pixelValues = new ArrayList<>();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = img.getRGB(x, y) & 0xFF;
                pixelValues.add(pixel / 4);
            }
        }
        return pixelValues;
    }

    public static List<int[]> reconstructList(BufferedImage img) {
        List<Integer> flatList = convertImageToList(img);
        List<int[]> reconstructedList = new ArrayList<>();
        for (int i = 0; i < flatList.size(); i += 11) {
            int[] tempArray = new int[11];
            for (int j = 0; j < 11 && (i + j) < flatList.size(); j++) {
                tempArray[j] = flatList.get(i + j);
            }
            reconstructedList.add(tempArray);
        }
        return reconstructedList;
    }

    public static List<int[]> encryptArray(int[] arr) {
        List<int[]> l = new ArrayList<>();
        for (int value : arr) {
            l.add(encryptInteger(value));
        }
        return l;
    }

    public static int[] encryptInteger(int num) {
        String binary = String.format("%10s", Integer.toBinaryString(num & 0x3FF)).replace(' ', '0');
        int[] fq = {31, 32, 60, 24, 21, 47, 53, 40, 26, 49, 45};
        int[] g = {1, 0, 1, 0, 1, 0, -1, 0, -1, -1, 0};
        int[] r = {3, 0, 0, -3, 3, 0, 0, 3, 0, -3, 0};
        int q = 61;
        int[] h = cyclic(g, fq);
        for (int i = 0; i < h.length; i++) h[i] = pythonModulo(h[i], 61);

        int[] m = new int[11];
        m[0] = (num < 0) ? 1 : 0;
        for (int i = 0; i < 10; i++) m[i + 1] = binary.charAt(i) - '0';
        for (int i = 0, j = m.length - 1; i < j; i++, j--) {
            int temp = m[i]; m[i] = m[j]; m[j] = temp;
        }

        int[] t = cyclic(r, h);
        int[] e = addPolynomials(t, m);
        for (int i = 0; i < e.length; i++) e[i] = pythonModulo(e[i], q);
        return e;
    }

    public static int[] centerToInterval(int divisor, int[] dividend) {
        int[] centered = Arrays.copyOf(dividend, dividend.length);
        for (int i = 0; i < centered.length; i++) {
            int mod = centered[i] % divisor;
            if (mod > divisor / 2) mod -= divisor;
            else if (mod <= -divisor / 2) mod += divisor;
            centered[i] = mod;
        }
        return centered;
    }

    public static List<Integer> decryptList(List<int[]> l) {
        int[] f = {1, 1, 1, 0, 1, 0, -1, 0, -1, 0, -1};
        int[] fp = {0, 1, 2, 2, 2, 1, 0, 1, 0, 1, 0};
        List<Integer> result = new ArrayList<>();
        for (int[] i : l) {
            int[] a = cyclic(f, i);
            for (int j = 0; j < a.length; j++) a[j] = pythonModulo(a[j], 61);
            a = centerToInterval(61, a);
            a = cyclic(fp, a);
            for (int j = 0; j < a.length; j++) a[j] = pythonModulo(a[j], 3);
            a = centerToInterval(3, a);
            int res = 0;
            for (int j = 0; j <= 9; j++) if (a[j] == 1) res += Math.pow(2, j);
            if (a[10] == 1) res = -res;
            result.add(res);
        }
        return result;
    }

    public static int[] cyclic(int[] a, int[] b) {
        int n = Math.max(a.length, b.length);
        int[] h = new int[n];
        for (int k = 0; k < n; k++) {
            h[k] = 0;
            for (int i = 0; i < n; i++) {
                int j = (k - i + n) % n;
                if (i < a.length && j < b.length) h[k] += a[i] * b[j];
            }
        }
        return h;
    }

    public static int[] addPolynomials(int[] poly1, int[] poly2) {
        int maxLength = Math.max(poly1.length, poly2.length);
        int[] result = new int[maxLength];
        for (int i = 0; i < maxLength; i++) {
            int coef1 = (i < poly1.length) ? poly1[i] : 0;
            int coef2 = (i < poly2.length) ? poly2[i] : 0;
            result[i] = coef1 + coef2;
        }
        return result;
    }

    public static int pythonModulo(int a, int b) {
        int mod = a % b;
        return (mod < 0) ? mod + Math.abs(b) : mod;
    }
    
    public static BufferedImage reconstructColorImage(List<Integer> redPixels, List<Integer> greenPixels, List<Integer> bluePixels, int height, int width) {
        BufferedImage reconstructedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        int index = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int r = Math.max(0, Math.min(255, redPixels.get(index)));
                int g = Math.max(0, Math.min(255, greenPixels.get(index)));
                int b = Math.max(0, Math.min(255, bluePixels.get(index)));
                Color pixelColor = new Color(r, g, b);
                reconstructedImage.setRGB(x, y, pixelColor.getRGB());
                index++;
            }
        }
        return reconstructedImage;
    }

    // Main Server Logic
    public static void main(String[] args) {
        Javalin app = Javalin.create(config -> {
            config.plugins.enableCors(cors -> cors.add(it -> it.anyHost()));
            config.http.maxRequestSize = 100 * 1024 * 1024; // 100 MB
        }).start(7070);

        System.out.println("✅ Encryption Server started on port 7070");

        // API Endpoint for ENCRYPTION
        app.post("/encrypt", ctx -> {
            UploadedFile uploadedFile = ctx.uploadedFile("image");
            if (uploadedFile == null) {
                ctx.status(400).result("No image file uploaded.");
                return;
            }

            try (var inputStream = uploadedFile.content()) {
                BufferedImage image = ImageIO.read(inputStream);
                
                ArrayList<Integer> redPixels = new ArrayList<>();
                ArrayList<Integer> greenPixels = new ArrayList<>();
                ArrayList<Integer> bluePixels = new ArrayList<>();

                for (int y = 0; y < image.getHeight(); y++) {
                    for (int x = 0; x < image.getWidth(); x++) {
                        Color color = new Color(image.getRGB(x, y));
                        redPixels.add(color.getRed());
                        greenPixels.add(color.getGreen());
                        bluePixels.add(color.getBlue());
                    }
                }

                List<int[]> encryptedRed = encryptArray(redPixels.stream().mapToInt(i -> i).toArray());
                List<int[]> encryptedGreen = encryptArray(greenPixels.stream().mapToInt(i -> i).toArray());
                List<int[]> encryptedBlue = encryptArray(bluePixels.stream().mapToInt(i -> i).toArray());
                
                List<Integer> flatRed = new ArrayList<>();
                encryptedRed.forEach(arr -> Arrays.stream(arr).forEach(flatRed::add));
                BufferedImage encryptedImageR = generateEncryptedImage(flatRed);

                List<Integer> flatGreen = new ArrayList<>();
                encryptedGreen.forEach(arr -> Arrays.stream(arr).forEach(flatGreen::add));
                BufferedImage encryptedImageG = generateEncryptedImage(flatGreen);
                
                List<Integer> flatBlue = new ArrayList<>();
                encryptedBlue.forEach(arr -> Arrays.stream(arr).forEach(flatBlue::add));
                BufferedImage encryptedImageB = generateEncryptedImage(flatBlue);

                Map<String, String> response = new HashMap<>();
                response.put("image_r", imageToBase64(encryptedImageR));
                response.put("image_g", imageToBase64(encryptedImageG));
                response.put("image_b", imageToBase64(encryptedImageB));
                response.put("width", String.valueOf(image.getWidth()));
                response.put("height", String.valueOf(image.getHeight()));

                ctx.json(response);

            } catch (Exception e) {
                e.printStackTrace();
                ctx.status(500).result("Error during encryption.");
            }
        });
        
        // NEW API Endpoint for DECRYPTION
        app.post("/decrypt", ctx -> {
            try {
                UploadedFile fileR = ctx.uploadedFile("image_r");
                UploadedFile fileG = ctx.uploadedFile("image_g");
                UploadedFile fileB = ctx.uploadedFile("image_b");
                int width = Integer.parseInt(ctx.formParam("width"));
                int height = Integer.parseInt(ctx.formParam("height"));

                if (fileR == null || fileG == null || fileB == null) {
                    ctx.status(400).result("Missing one or more encrypted channel files.");
                    return;
                }

                BufferedImage imgR = ImageIO.read(fileR.content());
                BufferedImage imgG = ImageIO.read(fileG.content());
                BufferedImage imgB = ImageIO.read(fileB.content());

                List<int[]> encryptedRed = reconstructList(imgR);
                List<int[]> encryptedGreen = reconstructList(imgG);
                List<int[]> encryptedBlue = reconstructList(imgB);

                List<Integer> decryptedRed = decryptList(encryptedRed);
                List<Integer> decryptedGreen = decryptList(encryptedGreen);
                List<Integer> decryptedBlue = decryptList(encryptedBlue);

                BufferedImage finalImage = reconstructColorImage(decryptedRed, decryptedGreen, decryptedBlue, height, width);
                
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(finalImage, "png", baos);
                ctx.contentType("image/png").result(baos.toByteArray());

            } catch (Exception e) {
                e.printStackTrace();
                ctx.status(500).result("Error during decryption.");
            }
        });
    }
    
    private static String imageToBase64(BufferedImage image) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        return Base64.getEncoder().encodeToString(baos.toByteArray());
    }
}
