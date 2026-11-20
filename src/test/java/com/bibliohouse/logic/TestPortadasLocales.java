/*
 * BiblioHouse - Un gestor de biblioteca personal.
 * Copyright (C) 2026 Fernando Lago Dávila
 *
 * Este programa es software libre: usted puede redistribuirlo y/o modificarlo
 * bajo los términos de la Licencia Pública General de GNU tal como se publica
 * por la Free Software Foundation, ya sea la versión 3 de la Licencia, o
 * (a su opción) cualquier versión posterior.
 *
 * Este programa se distribuye con la esperanza de que sea útil, pero
 * SIN NINGUNA GARANTÍA; sin siquiera la garantía implícita de
 * COMERCIABILIDAD o APTITUD PARA UN PROPÓSITO PARTICULAR. Vea la
 * Licencia Pública General de GNU para más detalles.
 *
 * Usted debería haber recibido una copia de la Licencia Pública General de GNU
 * junto con este programa. Si no es así, vea <https://www.gnu.org/licenses/>.
 */
package com.bibliohouse.logic;

import com.bibliohouse.utils.ImageLoader;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de la copia local de portadas: formatos aceptados y reutilización de
 * la caché de vista previa al guardar.
 *
 * @author ferlagod (Fernando Lago Dávila)
 * @version 2.3
 */
class TestPortadasLocales {

    @TempDir
    File tempDir;

    private static void escribirImagen(File destino, String formato) throws Exception {
        BufferedImage img = new BufferedImage(200, 300, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < 200; x++) {
            for (int y = 0; y < 300; y++) {
                img.setRGB(x, y, (x * 7 + y * 13) & 0xFFFFFF);
            }
        }
        assertTrue(ImageIO.write(img, formato, destino));
    }

    @Test
    void portadaGifSeCopiaALaBiblioteca() throws Exception {
        File origen = new File(tempDir, "portada.gif");
        escribirImagen(origen, "gif");
        File usuario = new File(tempDir, "usuario");

        String ruta = ImageLoader.hacerPortadaLocalOffline(origen.getAbsolutePath(), "libro-gif", usuario.getAbsolutePath());

        assertEquals(new File(new File(usuario, "covers"), "libro-gif.jpg").getAbsolutePath(), ruta,
                "Un GIF debe convertirse a covers/<id>.jpg y no quedarse apuntando al original");
        assertNotNull(ImageIO.read(new File(ruta)));
    }

    @Test
    void guardarReutilizaLaPortadaYaDescargadaEnCache() throws Exception {
        File usuario = new File(tempDir, "usuario");
        File covers = new File(usuario, "covers");
        ImageLoader.setCacheDir(covers.getAbsolutePath());

        // URL sin resolución DNS: si se intentara descargar, fallaría
        String url = "https://portada.invalid/libro.jpg";
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        StringBuilder hex = new StringBuilder();
        for (byte b : sha.digest(url.getBytes(StandardCharsets.UTF_8))) {
            hex.append(String.format("%02x", b));
        }
        escribirImagen(new File(covers, hex + ".png"), "png");

        String ruta = ImageLoader.hacerPortadaLocalOffline(url, "libro-url", usuario.getAbsolutePath());

        assertEquals(new File(covers, "libro-url.jpg").getAbsolutePath(), ruta);
        assertNotNull(ImageIO.read(new File(ruta)));
    }
}
