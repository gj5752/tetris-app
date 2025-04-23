package tetris

import scala.collection.immutable.Range
import scala.util.Random
import scala.math.max

import sdraw._

// テトロミノを操作するための関数
object ShapeLib {
  // 色とブロックの表現
  type ColorSymbol = Char

  val blockSymbols = List('I', 'J', 'T', 'O', 'Z', 'L', 'S')
  val blockColors = {
    Vector(HSB(0.5f, 0.3f, 0.95f), HSB(0.61f, 0.6f, 1f), HSB(0.84f, 0.58f, 0.94f),
    HSB(0.16f, 0.67f, 0.97f), HSB(0.97f, 0.65f, 0.90f), HSB(0.07f, 0.64f, 0.96f),
    HSB(0.36f, 0.52f, 0.94f))
  }
  val colorSymbols = blockSymbols ++ List('G', 'g')
  val colors = blockColors ++ List(DarkGray, LightGray)
  val Color2Sym = colors.zip(colorSymbols).toList

  val Sym2Color: List[(ColorSymbol, Color)] =
    Color2Sym.map(cn => (cn._2, cn._1))

  // テトロミノの表現
  type Block = Color
  type Row = List[Block]
  type Shape = List[Row]
  type ShapeSpec = List[String]

  // テトロミノの定義
  val shapeSpecs: List[ShapeSpec] =
    List(
      List("IIII"),
      List("J  ", "JJJ"),
      List(" T ", "TTT"),
      List("OO", "OO"),
      List("ZZ ", " ZZ"),
      List("  L", "LLL"),
      List(" SS", "SS "))

  def make(spec: ShapeSpec): Shape = {

    def color(c: ColorSymbol): Color =
      Sym2Color.find(p => p._1.equals(c)) match {
        case Some((_, c)) => c
        case _ => Transparent
      }

    spec.map((row: String) => row.toList.map(color))
  }

  // 7種類のテトロミノが入ったリスト
  val allShapes: List[Shape] = shapeSpecs.map(make)
  val List(shapeI, shapeJ, shapeT, shapeO, shapeZ, shapeL, shapeS) = allShapes

  // 次のテトロミノの選択
  val r = new Random()

  def random(): List[Shape] = r.shuffle(allShapes)

  // n 個の a からなるリストを作る
  def duplicate[A](n: Int, a: A): List[A] = {
    List.fill(n)(a)
  }

  // rows 行 cols 列の空の shape を作る
  def empty(rows: Int, cols: Int): Shape = {
    duplicate[Row](rows, duplicate[Block](cols, Transparent))
  }

  // shape の大きさを (行数, 列数) の形で返す
  def size(shape: Shape): (Int, Int) = {
    (shape.length, shape.foldLeft(0)((r: Int, x: Row) => max(r, x.length)))
  }

  // 空でないブロックの数を返す
  def blockCount(shape: Shape): Int = {
    shape match {
      case Nil => 0
      case x::xs =>
        x.foldLeft(0)(
          (r: Int, y: Block) =>
            if (y != Transparent) r + 1
            else r
        ) + blockCount(xs)
    }
  }

  // まっとうな shape であるかどうかを判定する
  def wellStructured(s: Shape): Boolean = {
    s match {
      case Nil => false
      case x::Nil => x != Nil
      case x::xs => (x.length == xs.head.length) && wellStructured(xs)
    }
  }

  // shape を反時計回りに 90° 回転させる
  def rotate(s: Shape): Shape = {
    assert(wellStructured(s))
    // リストの要素を順番ひっくり返して、各要素をListの中にいれて提出する関数
    def inListReverseList[A](l: List[A]): List[List[A]] = {
      l match {
        case Nil => Nil
        case x::xs => inListReverseList(xs) ++ List(List(x))
      }
    }
    // あるShapeの一番左に「列」をつなげる関数
    def connectCol(c: Shape, s: Shape): Shape = {
      (c, s) match {
        case (x::xs, y::ys) => (x ++ y)::connectCol(xs, ys)
        case _ => Nil
      }
    }
    s match {
      case Nil => Nil
      case x::Nil => inListReverseList(x)
      case x::xs => connectCol(inListReverseList(x), rotate(xs))
    }
  }

  // shape s を右に x,下に y ずらす
  def shiftSE(s: Shape, x: Int, y: Int): Shape = {
    // sの行を行数、xを列数とする空のShapeをsの左につなげる関数
    def connectShapeEmpL(s: Shape, x: Int): Shape = {
      s match {
        case Nil => Nil
        case z::zs =>
          (duplicate(x, Transparent) ++ z) :: connectShapeEmpL(zs, x)
      }
    }
    empty(y, x + size(s)._2) ++ connectShapeEmpL(s, x)
  }

  // shape を左に x, 上に y ずらす
  def shiftNW(s: Shape, x: Int, y: Int): Shape = {
    val n = x + size(s)._2
    // sの右側に空のShapeを結合し、sの最大列数からxだけ範囲を増やす関数
    def connectShapeEmpR(s: Shape, x: Int): Shape = {
      s match {
        case Nil => Nil
        case z::zs =>
          (z ++ duplicate(n - z.length, Transparent))::connectShapeEmpR(zs, x)
      }
    }
    connectShapeEmpR(s, x) ++ empty(y, n)
  }

  // shape を rows 行 cols 列に拡大する
  def padTo(s: Shape, rows: Int, cols: Int): Shape = {
    assert((rows >= s.length) && (cols >= size(s)._2))
    shiftNW(s, cols - size(s)._2, rows - s.length)
  }

  // 2つの shape に重なりがあるかを判定する
  def overlap(s1: Shape, s2: Shape): Boolean = {
    // 2つの行に重なりがあるか判定する関数
    def overlapRow(r1: Row, r2: Row): Boolean = {
      (r1, r2) match {
        case (Nil, Nil) => false
        case (Nil, r2) => false
        case (r1, Nil) => false
        case (x::xs, y::ys)=> (x != Transparent && y != Transparent) || overlapRow(xs, ys)
      }
    }
    (s1, s2) match {
      case (Nil, Nil) => false
      case (Nil, s2) => false
      case (s1, Nil) => false
      case (x::xs, y::ys) => overlapRow(x, y) || overlap(xs, ys)
    }
  }

  // 2つの shape を結合する
  def combine(s1: Shape, s2: Shape): Shape = {
    assert(overlap(s1, s2) == false)
    // Row型の2つのデータを重ねる関数
    def combineRow(r1: Row, r2: Row): Row = {
      (r1, r2) match {
        case (Nil, Nil) => Nil
        case (x::xs, y::ys) =>
          if (x != Transparent) x::combineRow(xs, ys)
          else y::combineRow(xs, ys)
        case _ => Nil
      }
    }
    val m1 = max(s1.length, s2.length)
    val m2 = max(size(s1)._2, size(s2)._2)
    val l1 = padTo(s1, m1, m2)
    val l2 = padTo(s2, m1, m2)
    // 行、列同じ大きさのShapeを結合する関数
    def combineSameSize(s1: Shape, s2: Shape): Shape = {
      (s1, s2) match {
        case (Nil, Nil) => Nil
        case (x::xs, y::ys) => combineRow(x, y)::combineSameSize(xs, ys)
        case _ => Nil
      }
    }
    combineSameSize(l1, l2)
  }
}
