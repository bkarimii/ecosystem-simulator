package uk.co.cpsd.javaproject1;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Font;

import java.net.URL;
import javax.swing.ImageIcon;
import javax.swing.JPanel;

public class WorldPanel extends JPanel {
    private final World world;
    private final Font font;

    private final Image goatImage;
    private final Image lionImage;

    public WorldPanel(World world) {
        this.world = world;
        setPreferredSize(new Dimension(700, 750));
        this.font = new Font("Arial", Font.PLAIN, 15);
        // Load goat image safely
        URL goatURL = getClass().getResource("/goat.png");

        if (goatURL == null) {
            throw new RuntimeException("Could not load goat.png");
        }
        this.goatImage = new ImageIcon(goatURL).getImage();

        // Load lion image safely
        URL lionURL = getClass().getResource("/bigCat.png");
        if (lionURL == null) {
            throw new RuntimeException("Could not load lion.png");
        }
        this.lionImage = new ImageIcon(lionURL).getImage();

    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        int cellSize = getWidth() / world.size;

        for (int x = 0; x < world.size; x++) {
            for (int y = 0; y < world.size; y++) {
                // Draw grass
                if (world.hasGrass(x, y)) {
                    g.setColor(Color.GREEN);
                    g.fillRect(x * cellSize, y * cellSize, cellSize, cellSize);
                } else {
                    g.setColor(Color.WHITE);
                    g.fillRect(x * cellSize, y * cellSize, cellSize, cellSize);
                }

                g.setColor(Color.GRAY);
                g.drawRect(x * cellSize, y * cellSize, cellSize, cellSize);
            }
        }

        world.animals().forEach(animal -> {

            Image animalImage = null;

            if (animal instanceof Goat) {
                animalImage = goatImage;
            } else if (animal instanceof Lion) {
                animalImage = lionImage;
            }

            if (animalImage != null) {
                g.drawImage(animalImage, animal.getX() * cellSize, animal.getY() * cellSize, cellSize, cellSize, this);
            }

            g.setColor(Color.BLACK);

            g.setFont(font);

            // Calculate text position to try and center it within the animal's drawn area
            String idText = String.valueOf(animal.getId());
            char genderChar = animal.getGender() == Goat.Gender.MALE ? 'M' : 'F';
            idText += genderChar;

            int textWidth = g.getFontMetrics().stringWidth(idText);
            int textHeight = g.getFontMetrics().getHeight();
            int textX = animal.getX() * cellSize + (cellSize / 2) - (textWidth / 2);
            int textY = animal.getY() * cellSize + (cellSize / 2) + (textHeight / 4);

            g.drawString(idText, textX, textY);
        });

        int scoreY = world.size * cellSize + 25;
        g.setColor(Color.BLACK);
        g.setFont(font);
        String statement = "Seconds elapsed: " + world.getTicksElapsed() + " Num of Lions is: "
                + world.lionCount() + " Num of Goats is:  "
                + world.goatCount() + " Num of Grass: " + world.grassCount();
        g.drawString(statement, cellSize, scoreY);
    }
}
