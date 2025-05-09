/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package vm.vmtrials.papers.weightingqueries2025;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import vm.datatools.Tools;
import vm.fs.FSGlobal;

/**
 *
 * @author au734419
 */
public class CreateComposedImagesOfTriplets {

    public static final String URL_ADDRESS = "https://disa.fi.muni.cz/profimedia/images/";
    public static final File DESTINATION = new File(FSGlobal.FOLDER_PLOTS, "Triplets_Markéta");
    public static final Float WIDTH_RATIO = 1.2f;
    public static final Float HEIGHT_RATIO = 1.2f;
    private static final Integer FONT_SIZE = 16;
    private static final Integer BORDER = 3;
    private static final Float RESERVATION_FOR_HEADERS = 1f;
    private final int tripletID;
    private final String qID;
    private final String o1ID;
    private final String o2ID;

    public static void main(String[] args) {
        DataParser parser = new DataParser();
        Map<Integer, ImageTriplet> triplets = parser.getTriplets();
        Iterator<Map.Entry<Integer, ImageTriplet>> it = triplets.entrySet().iterator();
        while (it.hasNext()) {
            ImageTriplet triplet = it.next().getValue();
            CreateComposedImagesOfTriplets composition = new CreateComposedImagesOfTriplets(triplet);
            try {
                BufferedImage composeTriplet = composition.composeTriplet();
                String name = vm.mathtools.Tools.formatFirstZeros(triplet.getTripletId(), 3);
                File dest = new File(DESTINATION, name + ".png");
                dest.getParentFile().mkdirs();
                Tools.storeImage(composeTriplet, dest);
            } catch (IOException ex) {
                Logger.getLogger(CreateComposedImagesOfTriplets.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public CreateComposedImagesOfTriplets(ImageTriplet triplet) {
        this(triplet.getTripletId(), triplet.getQueryImageId(), triplet.getLeftImageId(), triplet.getRightImageId());
    }

    public CreateComposedImagesOfTriplets(int tripletID, String qID, String o1ID, String o2ID) {
        this.tripletID = tripletID;
        this.qID = vm.mathtools.Tools.formatFirstZeros(Integer.parseInt(qID), 10);
        this.o1ID = vm.mathtools.Tools.formatFirstZeros(Integer.parseInt(o1ID), 10);
        this.o2ID = vm.mathtools.Tools.formatFirstZeros(Integer.parseInt(o2ID), 10);
    }

    public BufferedImage composeTriplet() throws IOException {
        URL urlQ = new URL(URL_ADDRESS + qID);
        URL urlO1 = new URL(URL_ADDRESS + o1ID);
        URL urlO2 = new URL(URL_ADDRESS + o2ID);
        BufferedImage q = Tools.loadImageFromUrl(urlQ);
        BufferedImage o1 = Tools.loadImageFromUrl(urlO1);
        BufferedImage o2 = Tools.loadImageFromUrl(urlO2);
        int[] dimensions = new int[2];
        int w1 = o1.getWidth();
        int w2 = o2.getWidth();
        int wq = q.getWidth();
        dimensions[0] = (int) ((w1 + w2) * WIDTH_RATIO) + 2 * BORDER;
        int h1 = o1.getHeight();
        int h2 = o2.getHeight();
        int hq = q.getHeight();
        dimensions[1] = (int) ((hq + Math.max(h1, h2)) * HEIGHT_RATIO) + 2 * FONT_SIZE + 2 * BORDER;
        int yo = (int) (dimensions[1] - Math.max(h1, h2) - FONT_SIZE * RESERVATION_FOR_HEADERS) - BORDER;
        int x2 = dimensions[0] - w2 - BORDER;
        BufferedImage result = new BufferedImage(dimensions[0], dimensions[1], BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = result.createGraphics();
        graphics.setPaint(Color.WHITE);
        graphics.fillRect(0, 0, dimensions[0], dimensions[1]);
        int xq = (dimensions[0] + w1 - w2 - wq) / 2;
        copyImage(q, result, xq, FONT_SIZE, "▼ q ▼", true);
        copyImage(o1, result, BORDER, yo, "▼ o1 ▼", true);
        copyImage(o2, result, x2, yo, "▼ o2 ▼", true);
        return result;
    }

    private void copyImage(BufferedImage source, BufferedImage dest, int x0InDest, int y0InDest, String header, boolean withBorder) {
        int width = source.getWidth();
        int height = source.getHeight();

        Raster sourceData = source.getData();
        int[] pixel = new int[3];
        int imageY0 = (int) (y0InDest + FONT_SIZE * RESERVATION_FOR_HEADERS);
        Graphics2D graphics = dest.createGraphics();
        graphics.setPaint(Color.BLACK);
        if (withBorder) {
            graphics.fillRect(x0InDest - BORDER, imageY0 - BORDER, width + 2 * BORDER, height + 2 * BORDER);
        }
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                pixel = sourceData.getPixel(x, y, pixel);
                Color c = new Color(pixel[0], pixel[1], pixel[2]);
                dest.setRGB(x + x0InDest, y + imageY0, c.getRGB());
            }
        }
        Font font = graphics.getFont().deriveFont(Font.BOLD, FONT_SIZE);
        graphics.setFont(font);
        FontMetrics fm = graphics.getFontMetrics();
        int stringWidth = fm.stringWidth(header);
        graphics.drawString(header, x0InDest - stringWidth / 2 + width / 2, y0InDest);
    }

    public int getTripletID() {
        return tripletID;
    }

    public String getTripletIDAsString() {
        return Integer.toString(tripletID);
    }

    public String getqID() {
        return qID;
    }

    public String getO1ID() {
        return o1ID;
    }

    public String getO2ID() {
        return o2ID;
    }

}
