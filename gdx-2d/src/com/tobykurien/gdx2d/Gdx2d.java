package com.tobykurien.gdx2d;

import java.lang.ref.WeakReference;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.GL10;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
import com.badlogic.gdx.physics.box2d.CircleShape;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Array;

public class Gdx2d implements ApplicationListener {
   private OrthographicCamera camera;
   private OrthographicCamera uiCamera;
   private SpriteBatch batch;

   private World world;
   private Box2DDebugRenderer debugRenderer;
   
   private WeakReference<Body> bottle;
   private long lastBallTime;
   private BitmapFont font;
   
   static final float WORLD_TO_BOX = 0.1f;
   static final float BOX_TO_WORLD = 10f;
   static final float CAMERA_SCALE = 3f;
   static final float UI_SCALE = 3f;

   @Override
   public void create() {
      float w = Gdx.graphics.getWidth();
      float h = Gdx.graphics.getHeight();

      camera = new OrthographicCamera(CAMERA_SCALE, h / w * CAMERA_SCALE);
      
      batch = new SpriteBatch();

      world = new World(new Vector2(0, -10), true);
      debugRenderer = new Box2DDebugRenderer();
      createBottle();
      
      // font and UI
      font = new BitmapFont(Gdx.files.internal("data/monaco.fnt"), 
               Gdx.files.internal("data/monaco.png"), false);
      uiCamera = new OrthographicCamera(512*UI_SCALE, h / w * 512*UI_SCALE);
      uiCamera.combined.setTranslation(-1, 1, 0);
   }

   @Override
   public void render() {
      Gdx.gl.glClearColor(0.3f, 0.3f, 0.3f, 1);
      Gdx.gl.glClear(GL10.GL_COLOR_BUFFER_BIT);

      if (Gdx.input.isButtonPressed(Input.Buttons.LEFT) && 
               (System.currentTimeMillis() - lastBallTime > 100)) {
         lastBallTime = System.currentTimeMillis();
         createBall();
         Gdx.app.log("world", "Bodies = " + world.getBodyCount());
      }
      
      world.step(1 / 60f, 6, 2);

      Array<Body> bi = new Array<Body>();
      bi.ordered = true;
      world.getBodies(bi);

      batch.setProjectionMatrix(camera.combined);
      //batch.enableBlending();
      batch.begin();

      renderBody(bottle.get(), (Sprite)bottle.get().getUserData());
      for (Body b : bi) {
         Sprite e = (Sprite) b.getUserData();
         if (b != bottle.get() && e != null) {
            renderBody(b, e);
         }
      }

      batch.setProjectionMatrix(uiCamera.combined);
      font.draw(batch, "Hello world", 100, -100);
      batch.end();
      
      debugRenderer.render(world, camera.combined);
   }

   private void renderBody(Body b, Sprite e) {
      // Update the entities/sprites position and angle
      Vector2 origin = new Vector2(e.getOriginX(), e.getOriginY());
      Vector2 pos = b.getPosition().sub(origin);
      
      if (pos.y < -CAMERA_SCALE) {
         // out of view
         world.destroyBody(b);
         Gdx.app.log("world", "Bodies = " + world.getBodyCount());
      } else {
         e.setPosition(pos.x, pos.y);
         // We need to convert our angle from radians to degrees
         e.setRotation(MathUtils.radiansToDegrees * b.getAngle());
         e.draw(batch);
      }
   }

   @Override
   public void resize(int width, int height) {
   }

   @Override
   public void pause() {
   }

   @Override
   public void resume() {
   }

   @Override
   public void dispose() {
      batch.dispose();
      font.dispose();

      Array<Body> bi = new Array<Body>();
      world.getBodies(bi);
      for (Body b : bi) {
         Sprite s = (Sprite) b.getUserData();
         if (s != null) s.getTexture().dispose();
      }

      world.dispose();
      debugRenderer.dispose();
   }


   private void createBottle() {
      float BOTTLE_WIDTH = 1;

      // Load the sprite
      Texture texture = new Texture(Gdx.files.internal("data/gfx/test01.png"));
      texture.setFilter(TextureFilter.Linear, TextureFilter.Linear);
      TextureRegion region = new TextureRegion(texture, 0, 0, texture.getWidth(), texture.getHeight());
      Sprite sprite = new Sprite(region);

      // 0. Create a loader for the file saved from the editor.
      BodyEditorLoader loader = new BodyEditorLoader(Gdx.files.internal("data/test.json"));

      // 1. Create a BodyDef, as usual.
      BodyDef bd = new BodyDef();
      bd.position.set(0, -1);
      bd.type = BodyType.StaticBody; // DynamicBody;

      // 2. Create a FixtureDef, as usual.
      FixtureDef fd = new FixtureDef();
      fd.density = 1;
      fd.friction = 0.5f;
      fd.restitution = 0.3f;

      // 3. Create a Body, as usual.
      Body bottleModel = world.createBody(bd);
      bottleModel.setUserData(sprite);
      bottle = new WeakReference<Body>(bottleModel);

      // 4. Create the body fixture automatically by using the loader.
      loader.attachFixture(bottleModel, "test01", fd, BOTTLE_WIDTH);

      // Reference the origin of the model
      Vector2 bottleModelOrigin = loader.getOrigin("test01", BOTTLE_WIDTH).cpy();
      sprite.setSize(BOTTLE_WIDTH, BOTTLE_WIDTH * sprite.getHeight() / sprite.getWidth());
      sprite.setOrigin(bottleModelOrigin.x, bottleModelOrigin.y);
   }

   public void createBall() {
      float BOTTLE_WIDTH = 0.03f;

      // 1. Create a BodyDef, as usual.
      BodyDef bd = new BodyDef();
      bd.type = BodyType.DynamicBody;

      // Create our body in the world using our body definition
      Body body = world.createBody(bd);
      
      // Create a circle shape and set its radius to 6
      CircleShape circle = new CircleShape();
      circle.setRadius(BOTTLE_WIDTH);

      // Create a fixture definition to apply our shape to
      FixtureDef fixtureDef = new FixtureDef();
      fixtureDef.shape = circle;
      fixtureDef.density = 0.7f;
      fixtureDef.friction = 0.4f;
      fixtureDef.restitution = 0.3f;

      // Create our fixture and attach it to the body
      body.createFixture(fixtureDef);

      // Remember to dispose of any shapes after you're done with them!
      // BodyDef and FixtureDef don't need disposing, but shapes do.
      circle.dispose();
      
      // create sprite for ball
      Pixmap pm = new Pixmap(16, 16, Pixmap.Format.RGBA8888);
      pm.setColor(1, 0, 0, 1);
      pm.fillCircle(pm.getWidth()/2, pm.getWidth()/2, pm.getWidth()/2);
      Sprite sprite = new Sprite(new Texture(pm));

      // size the sprite to match the body def above
      sprite.setSize(0.06f, 0.06f * sprite.getHeight() / sprite.getWidth());
      
      // origin of this sprite (it's center) needs to overlap the 
      // center of the circle in the body def above
      sprite.setOrigin(sprite.getHeight()/2, sprite.getHeight()/2);
      body.setUserData(sprite);
      pm.dispose();
      
      // place ball above bottle
      body.setTransform(0, 1, 0);
   }
}
