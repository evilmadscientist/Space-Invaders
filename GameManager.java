//ProjA5: Project 1 Complete
//Space Invaders by Mario Ruiz and Maxwell Yun
//Use the arrow keys/WASD and the space bar to move and shoot.  Press P to pause.
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.KeyAdapter;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JComponent;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.util.ArrayList;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.lang.Runnable;
import java.awt.GraphicsEnvironment;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;

/**
 * The GameManager class manages the overall game and each of the game's components.
 * It contains the main loop of the program.
*/
public class GameManager extends JComponent {
   static JFrame frame;
   static GraphicsManager g;
   static Ship ship;
   static int numLives = 3;
   static int score = 0;
   static boolean paused = false;
   static boolean gameWon = false;
   static Runnable gameLoop;
   static ScheduledExecutorService executor;
   static ScheduledFuture<?> loopScheduler;
   public final static int width = 600;
   public final static int height = 600;
   public final static double particleSpeed = 3;
   
   public static void main(String[] args)  {
      frame = new JFrame();
      frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      frame.setSize(width, height);
      frame.getContentPane().setBackground(Color. black);
      frame.setResizable(false);
    
      g = new GraphicsManager();
    
      ship = new Ship("images/ship.png", 90, height - 150, 40, 20);
      frame.addKeyListener((KeyListener) ship); 
      frame.setVisible(true);
    
      AlienManager.createAliens();
      DefenseManager.createDefenses();
      UIManager.initialize();
    
      gameLoop = new Runnable() {
         public void run() {
            refresh();
         }
      };
   
      executor = Executors.newScheduledThreadPool(1);
      start();
   }
  
   public static void refresh() {
      frame.remove(g);
    
      if(!paused) {
         ship.update();
         AlienManager.update();
         DefenseManager.update();
      }
    
      frame.add(g);
      frame.revalidate();
      frame.repaint(); 
   }
  
   public static Ship getShip() {
      return ship;
   }
  
   public static void shipHit() {
      stop();
      try {
         Thread.sleep(1000);
      } catch(Exception e) {
         e.printStackTrace();
      }
      numLives--;
      ship.reset();
      if(numLives > 0) {
         start();
      } else {
         refresh();
      }
   }
  
   public static void incrScore(int amount) {
      score += amount;
   }
  
   public static int getScore() {
      return score;
   }
  
   public static int getLives() {
      return numLives;
   }
  
   public static void gameOver() {
      stop();
      paused = true;
      numLives = 0;
      refresh();
   }
   
   public static void gameWon() {
      stop();
      paused = true;
      gameWon = true;
      refresh();
   }
  
   public static void pause() {
      paused = !paused;
   }
  
   public static boolean isPaused() {
      return paused;
   }
   
   public static boolean isWon() {
      return gameWon;
   }
  
   public static void start() {
      loopScheduler = executor.scheduleAtFixedRate(gameLoop, 0, 8, TimeUnit.MILLISECONDS);
   }
  
   public static void stop() {
      loopScheduler.cancel(false);
   }
}

/**
 * The Ship class represents the player's ship.  It handles controls as well.
*/
class Ship extends Sprite implements KeyListener {
   int left;
   int right;
   final double speed = 2;
   Projectile shot;
   private boolean firing = false;
    
   public Ship(String imgUrl, int x, int y, int w, int h) {
      super(imgUrl, x, y, w, h);
   }
    
   public void reset() {
      position.x = 90;
   }
    
   public void keyTyped(KeyEvent e) {}

   public void keyPressed(KeyEvent e) {
      if(e.getKeyCode() == KeyEvent.VK_LEFT || e.getKeyCode() == KeyEvent.VK_A){
         left = 1;
      }
      if(e.getKeyCode() == KeyEvent.VK_RIGHT || e.getKeyCode() == KeyEvent.VK_D){
         right = 1;
      }
      if(e.getKeyCode() == KeyEvent.VK_SPACE || e.getKeyCode() == KeyEvent.VK_DOWN) {
         firing = true;
      }
   }
  
   public void keyReleased(KeyEvent e) {
      if(e.getKeyCode() == KeyEvent.VK_LEFT || e.getKeyCode() == KeyEvent.VK_A){
         left = 0;
      }
      if(e.getKeyCode() == KeyEvent.VK_RIGHT || e.getKeyCode() == KeyEvent.VK_D){
         right = 0;
      }
      if(e.getKeyCode() == KeyEvent.VK_SPACE || e.getKeyCode() == KeyEvent.VK_DOWN) {
         firing = false;
      }  
      if(e.getKeyCode() == KeyEvent.VK_P){
         GameManager.pause();
      }
   }
    
   public void update() {
      position.x += (right - left) * speed;
      position.x = (position.x < 10) ? 10 : position.x;
      position.x = (position.x > GameManager.width - width - 10) ? GameManager.width - width - 10 : position.x;
      if(firing) {
         fire();
      }
      if(shot != null) {
         shot.update();
         if(shot.hasHit()) {
            shot = null;
         }
      }
   }
    
   public void fire() {
      if(shot == null) {
         playSound("sounds/shoot.wav");
         shot = new Projectile(-1, position.x+18, position.y - 15);
      }
   }
    
   public void draw(Graphics2D g2d) {
      super.draw(g2d);
      if(shot != null)
         shot.draw(g2d);
   }
}

/**
 * The GraphicsManager class manages the game's graphics.  It contains the paintComponent method
 * that is used to draw every frame of the game.
*/
class GraphicsManager extends JComponent {
   public void paintComponent (Graphics g) {
      Graphics2D g2d = (Graphics2D) g;
      
      g.setColor(Color.green);
      g2d.setStroke(new BasicStroke(3));
      g2d.drawLine(10, GameManager.height-100, GameManager.width-10, GameManager.height-100);
      
      if(!GameManager.isPaused()) {
         GameManager.getShip().draw(g2d);
         DefenseManager.draw(g2d);
         AlienManager.draw(g2d);
      }
      UIManager.draw(g2d);
   }
   
}

/**
 * The Projectile class represents the game's projectiles (both player and enemy). It also checks
 * for collisions between projectiles and defenses, enemy ships, and the player's ship.
*/
class Projectile extends Sprite {
   protected int yDirection;
   
   public Projectile(int direction, int x, int y) {
      super("images/shot.png", x, y, 4, 15);
      yDirection = direction;
   }
   
   public void update() {
      position.y += yDirection * GameManager.particleSpeed * ((yDirection < 0) ? 1 : 0.7);
   }
   
   public boolean hasHit() {
      if(position.y < 0 || position.y > GameManager.height - 150) 
         return true;
      if(Sprite.isColliding(this, GameManager.getShip())) {
         playSound("sounds/explosion.wav");
         GameManager.shipHit();
         return true;
      }
      ArrayList<DefenseBlock> defenses = DefenseManager.getDefenses();
      for(DefenseBlock d : defenses) {
         if(Sprite.isColliding(this, d)) {
            d.hit();
            return true;
         }
      }
      if(yDirection < 0) {
         ArrayList<Alien> aliens = AlienManager.getAliens();
         for(Alien alien : aliens) {
            if(Sprite.isColliding(this, alien)) {
               alien.hit();
               playSound("sounds/invaderkilled.wav");
               return true;
            }
         }
      }
      return false;
   }
   
}

/**
 * The Sprite class is an abstract class that is extended by every Sprite in the game.  It provides
 * basic drawing, sound playing, and collision detection methods.
*/
abstract class Sprite {
   protected BufferedImage image;
   protected Point position;
   protected int width;
   protected int height;
   
   public Sprite(String imgUrl, int x, int y, int w, int h) {
      position = new Point(x,y);
      try {
         image = ImageIO.read(new File(imgUrl));
      } catch(Exception e) {
         e.printStackTrace();
      }
      width = w;
      height = h;
   }
   
   abstract void update();
   
   public void draw(Graphics2D g2d) {
      g2d.drawImage(image, (int)position.x, (int)position.y, width, height, null); 
   }
   
   public Point getPosition() {
      return position;
   }
   
   public int getWidth() {
      return width;
   }
   
   public int getHeight() {
      return height;
   }
   
   public static void playSound(String filename) {
       try {
           Clip clip = AudioSystem.getClip();
           clip.open(AudioSystem.getAudioInputStream(new File(filename)));
           clip.start();
       } catch (Exception e) {
           e.printStackTrace();
       }
   }
   
   public static boolean isColliding(Sprite s1, Sprite s2) {
      return !(s1.position.x+s1.width < s2.position.x || s2.position.x+s2.width < s1.position.x || s1.position.y+s1.height<s2.position.y||s2.position.y+s2.height<s1.position.y); 
   }
}

/**
 * The Alien class is an abstract class that is extended by every type of Alien in the game.
 * It contains the bulk of the code for alien movement and displaying.
*/
abstract class Alien extends Sprite {
   protected BufferedImage walkingImage;
   private boolean isWalking;
   protected boolean isHit = false;
   private int score;
   
   public Alien(String img1, String img2, int x, int y, int w, int h, int s) {
      super(img1, x, y, w, h);
      score = s;
      try {
         walkingImage = ImageIO.read(new File(img2));
      } catch(Exception e) {
         e.printStackTrace();
      }
   } 
   
   public void draw(Graphics2D g2d) {
      if(isWalking) {
         g2d.drawImage(walkingImage, (int)position.x, (int)position.y, width, height, null);
      } else {
         g2d.drawImage(image, (int)position.x, (int)position.y, width, height, null);
      }
   }  
   
   public void update() {
      isWalking = !isWalking;
      position.x += 8 * AlienManager.getDirection();
   }
   
   public void hit() {
      isHit = true;
      GameManager.incrScore(score);
   }
   
   public boolean isHit() {
      return isHit;
   }
}

/**
 * The AlienA class represents the first type of alien in the game.
*/
class AlienA extends Alien {
   public AlienA(int x, int y) {
      super("images/alien1-1.png", "images/alien1-2.png", x, y, 22, 22, 30);
   } 
}

/**
 * The AlienB class represents the second type of alien in the game.
*/
class AlienB extends Alien {
   public AlienB(int x, int y) {
      super("images/alien2-1.png", "images/alien2-2.png", x, y, 30, 20, 20);
   } 
}

/**
 * The AlienC class represents the third type of alien in the game.
*/
class AlienC extends Alien {
   public AlienC(int x, int y) {
      super("images/alien3-1.png", "images/alien3-2.png", x, y, 30, 20, 10);
   } 
}

/**
 * The AlienManager class manages the creation, movement, firing, and displaying for all
 * aliens in the game.
*/
class AlienManager {
   static ArrayList<Alien> aliens = new ArrayList<Alien>();
   static ArrayList<Projectile> shots = new ArrayList<Projectile>();
   static int direction = 1;
   static int walkTime = 150;
   private static int frameCount = 0;
   private static int moveCount = 0;
   
   public static void createAliens() {
      for(int i=0; i<11; i++)
         aliens.add(new AlienA(i*40 + 20, 100));
      for(int i=0; i<11; i++) {
         aliens.add(new AlienB(i*40 + 15, 135));
         aliens.add(new AlienB(i*40 + 15, 170));
      }
      for(int i=0; i<11; i++) {
         aliens.add(new AlienC(i*40 + 15, 205));
         aliens.add(new AlienC(i*40 + 15, 240));
      }

   }
   
   public static void update() {
      frameCount = (frameCount > 2 * AlienManager.getWalkTime()) ? 0 : frameCount+1;
      for(int i=0; i<aliens.size(); i++) {
         if(aliens.get(i).isHit()) {
            aliens.remove(i);
            i--;
         }
      }
      if(aliens.size() == 0) {
         GameManager.gameWon();
         return;
      }
      for(int i=0; i<shots.size(); i++) {
         shots.get(i).update();
         if(shots.get(i).hasHit()) {
            shots.remove(i);
            i--;
         }
      }
      if(Math.random()*300 < 1) {
         fire(aliens.get((int)(Math.random()*aliens.size())));
      }
      if(frameCount == walkTime || frameCount == walkTime * 2) {
         moveCount = (moveCount < 4) ? moveCount + 1 : 1;
         playSound("sounds/fastinvader" + moveCount + ".wav");
         for(Alien alien: aliens) {
            if(alien.position.x < 25) {
               if(direction == 0)
                  direction = 1;
               if(direction == -1) {
                  for(Alien a: aliens) {
                     a.position.y += 25;
                     if(a.position.y > GameManager.height - 220) {
                        GameManager.gameOver();
                     }
                  }   
                  walkTime *= .95;
                  direction = 0;
               }
               break;
            }
            if(alien.position.x > GameManager.width-45) {
               if(direction == 0)
                  direction = -1;
               if(direction == 1) {
                  for(Alien a: aliens) {
                     a.position.y += 25;
                     if(a.position.y > GameManager.height - 220) {
                        GameManager.gameOver();
                     }
                  } 
                  walkTime *= .95;
                  direction = 0;
               }
               break;
            } 
         }
         for(Alien alien: aliens) {
            alien.update();
         }
      }
   }
   
   public static void draw(Graphics2D g2d) {
      for(Alien alien: aliens) {
         alien.draw(g2d);
      }
      for(Projectile shot: shots) {
         shot.draw(g2d);
      }
   }
   
   static int getWalkTime() {
      return walkTime;
   }
   
   static int getDirection() {
      return direction;
   }
   
   static ArrayList<Alien> getAliens() {
      return aliens;
   }
   
   public static void fire(Alien a) {
      shots.add(new Projectile(1, a.getPosition().x + a.getWidth()/2,  a.getPosition().y + a.getHeight()/2));
      playSound("sounds/shoot.wav");
   }
   
   public static void playSound(String filename) {
       try {
           Clip clip = AudioSystem.getClip();
           clip.open(AudioSystem.getAudioInputStream(new File(filename)));
           clip.start();
       } catch (Exception e) {
           e.printStackTrace();
       }
   }
}

/**
 * The DefenseBlock class is an abstract class extended by every defense block in the game.  
 * It provide the bulk of the code for defenses and handles drawing and defense strength. 
*/
abstract class DefenseBlock extends Sprite {
   protected int strength = 4;
   protected BufferedImage img2;
   protected BufferedImage img3;
   protected BufferedImage img4;
   
   public DefenseBlock(String i1, String i2, String i3, String i4, int x, int y, int w, int h) {
      super(i1, x, y, w, h);
      try {
         img2 = ImageIO.read(new File(i2));
         img3 = ImageIO.read(new File(i3));
         img4 = ImageIO.read(new File(i4));
      } catch(Exception e) {
         e.printStackTrace();
      }
   }
   
   public void update() { 
   }
   
   public void draw(Graphics2D g2d) {
      switch(strength) {
         case 4:
            g2d.drawImage(image, (int)position.x, (int)position.y, width, height, null);
            break;
         case 3:
            g2d.drawImage(img2, (int)position.x, (int)position.y, width, height, null);
            break;
         case 2:
            g2d.drawImage(img3, (int)position.x, (int)position.y, width, height, null);
            break;
         case 1:
            g2d.drawImage(img4, (int)position.x, (int)position.y, width, height, null);
            break;
      }
   }  
   
   public void hit() {
      strength--;
   }
   
   public boolean isDestroyed() {
      return strength <= 0;
   }
}

/**
 * The DefenseManager class manages the creation, destruction, and displaying for all
 * defenses and defense blocks in the game.
*/
class DefenseManager {
   static ArrayList<DefenseBlock> defenses = new ArrayList<DefenseBlock>();
   
   public static void createDefenses() {
      createBlock(80, 350);
      createBlock(205, 350);
      createBlock(335, 350);
      createBlock(460, 350);
   }
   
   public static void createBlock(int x, int y) {
      int s = 15;
      defenses.add(new DefenseBlockD(x,y));
      defenses.add(new DefenseBlockA(s+x,y));
      defenses.add(new DefenseBlockA(2*s+x,y));
      defenses.add(new DefenseBlockE(3*s+x,y));
      defenses.add(new DefenseBlockA(x,y+s));
      defenses.add(new DefenseBlockB(s+x,y+s));
      defenses.add(new DefenseBlockC(2*s+x,y+s));
      defenses.add(new DefenseBlockA(3*s+x,y+s));
      defenses.add(new DefenseBlockA(x,y+2*s));
      defenses.add(new DefenseBlockA(3*s+x,y+2*s));
   }
   
   public static void update() {
      for(int i=0; i<defenses.size(); i++) {
         if(defenses.get(i).isDestroyed()) {
            defenses.remove(i);
            i--;
         }
      }
   }

   
   public static void draw(Graphics2D g2d) {
      for(DefenseBlock d: defenses) {
         d.draw(g2d);
      }
   }
   
   public static ArrayList<DefenseBlock> getDefenses() {
      return defenses;
   }
}

/**
 * The DefenseBlockA class represents the first type of defense block in the game.
*/
class DefenseBlockA extends DefenseBlock {
   public DefenseBlockA(int x, int y) {
      super("images/defense1-1.png", "images/defense1-2.png", "images/defense1-3.png", "images/defense1-4.png",
         x, y, 15, 15);
   }
}

/**
 * The DefenseBlockB class represents the second type of defense block in the game.
*/
class DefenseBlockB extends DefenseBlock {
   public DefenseBlockB(int x, int y) {
      super("images/defense2-1.png", "images/defense2-2.png", "images/defense2-3.png", "images/defense2-4.png",
         x, y, 15, 15);
   }
}

/**
 * The DefenseBlockC class represents the third type of defense block in the game.
*/
class DefenseBlockC extends DefenseBlock {
   public DefenseBlockC(int x, int y) {
      super("images/defense3-1.png", "images/defense3-2.png", "images/defense3-3.png", "images/defense3-4.png",
         x, y, 15, 15);
   }
}

/**
 * The DefenseBlockD class represents the fourth type of defense block in the game.
*/
class DefenseBlockD extends DefenseBlock {
   public DefenseBlockD(int x, int y) {
      super("images/defense4-1.png", "images/defense4-2.png", "images/defense4-3.png", "images/defense4-4.png",
         x, y, 15, 15);
   }
}

/**
 * The DefenseBlockE class represents the fifth type of defense block in the game.
*/
class DefenseBlockE extends DefenseBlock {
   public DefenseBlockE(int x, int y) {
      super("images/defense5-1.png", "images/defense5-2.png", "images/defense5-3.png", "images/defense5-4.png",
         x, y, 15, 15);
   }
}

/**
 * The UIManager class manages the game's UI (score, lives, Game Over/Pause screen).
*/
class UIManager {
   private static BufferedImage img;
   
   public static void initialize() {
      try {
         GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
         ge.registerFont(Font.createFont(Font.TRUETYPE_FONT, new File("fonts/SpaceInvaders.ttf")));
         img = ImageIO.read(new File("images/ship.png"));
      } catch (Exception e) {
         e.printStackTrace();
      }
   }

   public static void draw(Graphics2D g2d) {
      g2d.setFont(new Font("Press Start 2P", Font.PLAIN, 15)); 
      g2d.setColor(Color.white);
      g2d.drawString("SCORE ", 20, 40);  
      g2d.setColor(Color.green);
      g2d.drawString("" + GameManager.getScore(), 110, 40); 
      
      g2d.setColor(Color.white);
      g2d.drawString("LIVES", GameManager.width-260, 40); 
      for(int i=0; i<GameManager.getLives(); i++) {
         g2d.drawImage(img, GameManager.width-165+(47*i), 21, 35, 17, null); 
      }
      
      if(GameManager.getLives() <= 0) {
         g2d.drawString("GAME OVER", GameManager.width/2 - 75, GameManager.height/2 - 50); 
      } else if(GameManager.isWon()) {
         g2d.drawString("YOU WIN!", GameManager.width/2 - 70, GameManager.height/2 - 50); 
      } else if(GameManager.isPaused()) {
         g2d.drawString("PAUSED", GameManager.width/2 - 50, GameManager.height/2 - 50); 
      }
   }
}