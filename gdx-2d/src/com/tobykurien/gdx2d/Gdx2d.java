package com.tobykurien.gdx2d;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL10;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Array;

public class Gdx2d implements ApplicationListener {
   private OrthographicCamera camera;
   private SpriteBatch batch;

   private World world;
   private Box2DDebugRenderer debugRenderer;
   static final float WORLD_TO_BOX = 0.1f;
   static final float BOX_TO_WORLD = 10f;
   static final float BOX_TO_CAMERA = 3f;

   @Override
   public void create() {
      float w = Gdx.graphics.getWidth();
      float h = Gdx.graphics.getHeight();

      camera = new OrthographicCamera(BOX_TO_CAMERA, h / w * BOX_TO_CAMERA);
      batch = new SpriteBatch();

      world = new World(new Vector2(0, -10), true);
      debugRenderer = new Box2DDebugRenderer();
      createBottle();
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
      bd.type = BodyType.StaticBody; //DynamicBody;

      // 2. Create a FixtureDef, as usual.
      FixtureDef fd = new FixtureDef();
      fd.density = 1;
      fd.friction = 0.5f;
      fd.restitution = 0.3f;

      // 3. Create a Body, as usual.
      Body bottleModel = world.createBody(bd);
      bottleModel.setUserData(sprite);

      // 4. Create the body fixture automatically by using the loader.
      loader.attachFixture(bottleModel, "test01", fd, BOTTLE_WIDTH);

      // Reference the origin of the model
      Vector2 bottleModelOrigin = loader.getOrigin("test01", BOTTLE_WIDTH).cpy();
      sprite.setSize(BOTTLE_WIDTH, BOTTLE_WIDTH * sprite.getHeight() / sprite.getWidth());
      sprite.setOrigin(bottleModelOrigin.x, bottleModelOrigin.y);

      // this stuff needs to be set in the render loop
//      Vector2 bottlePos = bottleModel.getPosition().sub(bottleModelOrigin);
//      sprite.setPosition(bottlePos.x, bottlePos.y);
//      sprite.setRotation(bottleModel.getAngle() * MathUtils.radiansToDegrees);
   }

   @Override
   public void dispose() {
      batch.dispose();
      
      Array<Body> bi = new Array<Body>();
      world.getBodies(bi);
      for (Body b : bi) {
         Sprite s = (Sprite) b.getUserData();
         if (s != null) s.getTexture().dispose();
      }
      
      world.dispose();
      debugRenderer.dispose();
   }

   @Override
   public void render() {
      Gdx.gl.glClearColor(0.3f, 0.3f, 0.3f, 1);
      Gdx.gl.glClear(GL10.GL_COLOR_BUFFER_BIT);

      world.step(1 / 60f, 6, 2);

      Array<Body> bi = new Array<Body>();
      world.getBodies(bi);

      batch.setProjectionMatrix(camera.combined);
      batch.enableBlending();
      batch.begin();

      for (Body b : bi) {
         // Get the bodies user data - in this example, our user
         // data is an instance of the Entity class
         Sprite e = (Sprite) b.getUserData();

         if (e != null) {
            // Update the entities/sprites position and angle
            Vector2 origin = new Vector2(e.getOriginX(), e.getOriginY());
            Vector2 pos = b.getPosition().sub(origin);
            e.setPosition(pos.x, pos.y);
            // We need to convert our angle from radians to degrees
            e.setRotation(MathUtils.radiansToDegrees * b.getAngle());
            e.draw(batch);
         }
      }

      batch.end();
      debugRenderer.render(world, camera.combined);
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
}
