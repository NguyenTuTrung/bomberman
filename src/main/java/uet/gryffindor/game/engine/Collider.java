package uet.gryffindor.game.engine;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import uet.gryffindor.game.base.GameObject;
import uet.gryffindor.game.base.Vector2D;
import uet.gryffindor.game.object.DynamicObject;

/**
 * Lớp máy va chạm giúp phát hiện va chạm.
 */
public class Collider {
  public final GameObject gameObject;

  private Vector2D dimension;
  public Vector2D position;

  private HashMap<Collider, Double> collidedList = new HashMap<>();
  private boolean isChangeByGameObject;

  public Collider(GameObject gameObject) {
    this.gameObject = gameObject;
    this.isChangeByGameObject = true;

    // Thuộc tính mặc định là thuộc tính của game object
    this.dimension = gameObject.dimension.clone();
    this.position = gameObject.position.clone();
  }

  public Collider(GameObject gameObject, Vector2D dimension) {
    this.gameObject = gameObject;
    this.dimension = dimension;
    this.isChangeByGameObject = false;
  }

  public void changeByGameObject(boolean value) {
    this.isChangeByGameObject = value;
  }

  public Vector2D getDimension() {
    return this.dimension;
  }

  public void setDimension(Vector2D dimension) {
    this.dimension.setValue(dimension.x, dimension.y);
    this.isChangeByGameObject = false;
  }

  /**
   * Lấy diện tích giao nhau giữa hai collider.
   * 
   * @param that collider muốn tìm diện tích giao nhau với collider hiện tại
   * @return diện tích giao nhau của hai collider
   */
  public double getOverlapArea(Collider that) {
    return this.collidedList.get(that);
  }

  /**
   * Hàm cân đối collider với game object (Vì tâm của collider phải trùng với tâm
   * của game object).
   * 
   * @return tọa độ trung tâm sau khi cân bằng
   */
  private Vector2D fitObject() {
    Vector2D center = gameObject.position.add(gameObject.dimension.multiply(0.5f));

    if (isChangeByGameObject) {
      this.dimension.setValue(gameObject.dimension.x, gameObject.dimension.y);
    }

    this.position = center.subtract(dimension.multiply(0.5f));
    return center;
  }

  /**
   * Tính diện tích giao nhau của hai collider.
   * 
   * @param that collider muốn kiểm tra
   * @return diện tích của vùng giao nhau
   */
  public double computeOverlapArea(Collider that) {
    this.fitObject();
    that.fitObject();

    Vector2D topLeft1 = this.position;
    Vector2D topLeft2 = that.position;

    Vector2D botRight1 = topLeft1.add(this.dimension);
    Vector2D botRight2 = topLeft2.add(that.dimension);

    // tìm tọa độ 4 đỉnh của hình chữ nhật giao
    double xLeft = Math.max(topLeft1.x, topLeft2.x);
    double xRight = Math.min(botRight1.x, botRight2.x);
    double yTop = Math.max(topLeft1.y, topLeft2.y);
    double yBottom = Math.min(botRight1.y, botRight2.y);

    if (xLeft < xRight && yTop < yBottom) {
      return (xRight - xLeft) * (yBottom - yTop);
    }

    return 0;
  }

  /**
   * Kiểm tra va chạm của tất cả các collider được khai báo. Nếu có hai collider
   * va chạm với nhau, các hàm {@link GameObject#onCollision(Collider)} của game
   * object chứa colldier sẽ được gọi.
   */
  public static void checkCollision(List<GameObject> objects) {
    var colliders = objects.stream().map(GameObject::getCollider).collect(Collectors.partitioningBy(col -> col.gameObject instanceof DynamicObject));
    
    List<Collider> dynamics = colliders.get(true);
    List<Collider> statics = colliders.get(false);

    // Check collision of dynamic objects
    for (int i = 0; i < dynamics.size(); i++) {
      for (int j = i + 1; j < dynamics.size(); j++) {
        checkTwoColliders(dynamics.get(i), dynamics.get(j));
      }
    }

    // Check collision of each pair of dynamic and static
    for (int i = 0; i < dynamics.size(); i++) {
      Collider a = dynamics.get(i);

      for (int j = 0; j < statics.size(); j++) {
        Collider b = statics.get(j);

        checkTwoColliders(a, b);
      }
    }
  }

  private static void checkTwoColliders(Collider a, Collider b) {
    if (a == b) {
      return;
    }

    double overlapArea = a.computeOverlapArea(b);
    // Nếu a va chạm với b
    if (overlapArea != 0) {

      if (!a.collidedList.containsKey(b) || a.collidedList.get(b) == 0) {
        // Nếu danh sách va chạm của a chưa có b
        // hoặc vùng giao nhau của a và b bằng không
        // thì gọi onCollsionEnter
        a.gameObject.onCollisionEnter(b);
        b.gameObject.onCollisionEnter(a);
      } else {
        // Nếu đã va chạm
        // thì gọi hàm onCollisionStay
        a.gameObject.onCollisionStay(b);
        b.gameObject.onCollisionStay(a);
      }
    } else {
      // Nếu vùng giao nhau trước đó khác 0
      // thì a và b mới bắt đầu rời va chạm
      if (a.collidedList.containsKey(b) && a.collidedList.get(b) != 0) {
        a.gameObject.onCollisionExit(b);
        b.gameObject.onCollisionExit(a);
      }
    }

    a.collidedList.put(b, overlapArea);
    b.collidedList.put(a, overlapArea);

  }
}