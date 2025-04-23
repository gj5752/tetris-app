package tetris

import scala.util.Random

import sgeometry.Pos
import sdraw.{World, Color, Transparent, HSB}

import tetris.{ShapeLib => S}

import sounds.Sound

// テトリスを動かすための関数
case class TetrisWorld(c: Int, l1: List[S.Shape], l2: List[S.Shape],
  m: Int, hold: S.Shape, piece: ((Int, Int), S.Shape),
  pile: S.Shape) extends World() {

  // マウスクリックは無視
  def click(p: sgeometry.Pos): World = this

  // ブロックの描画
  def drawRect(x: Int, y: Int, w: Int, h: Int, c: Color): Boolean = {
    canvas.drawRect(Pos(A.BlockSize * x, A.BlockSize * y),
      A.BlockSize * w, A.BlockSize * h, c)
  }

  // shape の描画（与えられた位置）
  def drawShape(pos: (Int, Int), shape: S.Shape): Boolean = {
    val pos_colors = shape.zipWithIndex.flatMap(row_i => {
      val (row, i) = row_i
      row.zipWithIndex.map(box_j => {
        val (color, j) = box_j
        (j, i, color)
      })
    })

    val (x, y) = pos
    pos_colors.forall(pos_color => {
      val (dx, dy, color) = pos_color
      drawRect(x + dx, y + dy, 1, 1, color)
    })
  }

  // 方眼の描画
  def drawGrid(color: Color): Boolean = {
    val xList = List.range(A.BlockSize, A.GameWidth, A.BlockSize)
    val yList = List.range(A.BlockSize, A.GameHeight, A.BlockSize)
    xList.forall(x => {
      canvas.drawLine(Pos(x, 0), Pos(x, A.GameHeight), color)
    }) &&
    yList.forall(y => {
      canvas.drawLine(Pos(0, y), Pos(A.GameWidth, y), color)
    }) &&
    canvas.drawRect(Pos(A.GameWidth, 0), A.BlockSize / 10, canvas.height, HSB(0, 0, 1f)) &&
    canvas.drawRect(Pos(A.GameWidth, A.BlockSize * 13 / 2), A.BlockSize * 6, A.BlockSize / 10, HSB(0, 0, 1f))

  }

  // ゴーストブロックの描画
  def drawGhost(pos: (Int, Int), shape: S.Shape): Boolean = {
    val (x, y) = pos
    val newY = colly(this, List.range(y, A.WellHeight)) - 1
    val shapeGhost = shape.map(row => row.map(b => b match {
      case HSB(x, y, z) => HSB(0, 0, 0.35f)
      case _ => b
    }))
    drawShape((x, newY), shapeGhost)
  }

  // ネクストブロックの描画
  def drawNext(c: Int): Boolean = {
    val l = Range(c+1, c+5).toList
    l.zipWithIndex.forall{y =>
      val (x, i) = y
      if (x > A.counter) {
        drawShape((11, 8 + 3*i), l2(x % (A.counter + 1)))
      }
      else {
        drawShape((11, 8 + 3*i), l1(x))
      }
    }
  }

  // shape の描画（原点）
  def drawShape00(shape: S.Shape): Boolean = drawShape((0, 0), shape)

  // ゲーム画面の描画
  val CanvasColor = HSB(0, 0, 0.1f)
  val GridColor = HSB(0, 0, 0.2f)

  def draw(): Boolean = {
    val (pos, shape) = piece
    canvas.drawRect(Pos(0, 0), canvas.width, A.GameHeight, CanvasColor) &&
    drawGrid(GridColor) &&
    drawShape00(pile) &&
    drawGhost(pos, shape) &&
    drawShape(pos, shape) &&
    drawShape((11, 1), hold) &&
    drawNext(c) &&
    canvas.drawString(Pos(A.GameWidth + A.BlockSize * 5 / 7, A.BlockSize * 5 / 7), "Hold") &&
    canvas.drawString(Pos(A.GameWidth + A.BlockSize * 5 / 7, A.BlockSize * 7 + A.BlockSize * 5 / 7), "Next")
  }

  // 時間の経過に応じて世界を更新する
  def tick(): World = {
    val ((x, y), s) = piece
    val nextTick = TetrisWorld(c, l1, l2, m, hold, ((x, y + 1), s), pile)
    if (collision(this)) {
      val se4 = new Sound(getClass.getResourceAsStream("/se_mod05.wav"))
      endOfWorld("Game Over")
      A.bgm.stop()
      canvas.drawRect(Pos((canvas.width - 400) / 2 - A.BlockSize, (canvas.height - 2 * 60) / 2 - A.BlockSize),
        400 + 2 * A.BlockSize, 60 + A.BlockSize, HSB(0, 0, 0))
      canvas.drawString(Pos((canvas.width - 400) / 2, (canvas.height - 60) / 2), "GAME OVER!!")
      se4.replay()
      this
    }
    else if (collision(nextTick)) {
      if (c >= A.counter) {
        var newMinos = S.random()
        if (l2.last == newMinos.head) {
          newMinos = newMinos.reverse
        }
        TetrisWorld(0, l2, newMinos, 0, hold, A.newPiece(0, l2),
          eraseRows(S.combine(S.shiftSE(s, x, y), pile)))
      }
      else {
        TetrisWorld(c + 1, l1, l2, 0, hold, A.newPiece(c + 1, l1),
          eraseRows(S.combine(S.shiftSE(s, x, y), pile)))
      }
    }
    else nextTick
  }

  // キー操作に応じて世界を更新する
  def keyEvent(key: String): World = {
    val ((x, y), s) = piece
    val wUp = TetrisWorld(c, l1, l2, m, hold, ((x, y), S.rotate(s)), pile)
    val wDown = TetrisWorld(c, l1, l2, m, hold, ((x, y + 1), s), pile)
    val wLeft = TetrisWorld(c, l1, l2, m, hold, ((x - 1, y), s), pile)
    val wRight = TetrisWorld(c, l1, l2, m, hold, ((x + 1, y), s), pile)
    val wCtrl = TetrisWorld(c, l1, l2, m, hold,
      ((x, y), S.rotate(S.rotate(S.rotate(s)))), pile)


    key match {
      case "UP"      =>
        if (collision(wUp))    this else {
          A.se1.replay()
          wUp}
      case "DOWN"    =>
        if (collision(wDown))  this else {
          A.se1.replay()
          wDown}
      case "LEFT"    =>
        if (collision(wLeft))  this else {
          A.se1.replay()
          wLeft}
      case "RIGHT"   =>
        if (collision(wRight)) this else {
          A.se1.replay()
          wRight}
      case "CONTROL" =>
        if (collision(wCtrl))  this else {
          A.se1.replay()
          wCtrl}
      case "SPACE"   => if (collision(this)) this else {
        if (c >= A.counter) {
          var newMinos = S.random()
          if (l2.last == newMinos.head) {
            newMinos = newMinos.reverse
          }
          A.se2.replay()
          TetrisWorld(0, l2, newMinos, 0, hold, A.newPiece(0, l2),
            eraseRows(S.combine(S.shiftSE(s, x,
              colly(this, List.range(y, A.WellHeight)) - 1), pile)))
        }
        else {
          A.se2.replay()
          TetrisWorld(c + 1, l1, l2, 0, hold, A.newPiece(c + 1, l1),
          eraseRows(S.combine(S.shiftSE(s, x,
            colly(this, List.range(y, A.WellHeight)) - 1), pile)))
        }
      }
      case "SHIFT" =>
        if (hold == Nil) {
          if (c >= A.counter) {
            var newMinos = S.random()
            if (l2.last == newMinos.head) {
              newMinos = newMinos.reverse
            }
            A.se3.replay()
            TetrisWorld(0, l2, newMinos, 1, s, A.newPiece(0, l2), pile)
          }
          else {
            A.se3.replay()
            TetrisWorld(c + 1, l1, l2, 1, s, A.newPiece(c + 1, l1), pile)
          }
        }
        else if (m == 0) {
          A.se3.replay()
          TetrisWorld(c, l1, l2, 1, s, ((A.WellWidth / 2 - 1, 0), hold), pile)
        }
        else this
      case _ => this
    }
  }

  // world で衝突が起きているかを判定する
  def collision(world: TetrisWorld): Boolean = {
    val ((x, y), s) = world.piece
    if (x < 0) true
    else S.overlap(S.shiftSE(s, x, y), pile) ||
      (x + S.size(s)._2 > A.WellWidth) ||
      (y + s.length > A.WellHeight)
  }

  // 揃った行を消す
  def eraseRows(pile: S.Shape): S.Shape = {
    // まず揃った行だけ取り除いたpileをつくる関数
    def removeRow(pile: S.Shape): S.Shape = {
      pile match {
        case Nil => Nil
        case x::xs =>
          if (S.blockCount(List(x)) == A.WellWidth)
            removeRow(xs)
          else x::removeRow(xs)
      }
    }
    // removeRowで揃った行を消し、その消した数だけ上につなげる
    val newpile = removeRow(pile)
    S.empty(A.WellHeight - newpile.length, A.WellWidth) ++ newpile
  }

  // 衝突のするy座標の計算
  // 引数のlistは現在のpieceのy座標からy座標の下限まで
  def colly(world: TetrisWorld, list: List[Int]): Int = {
    val TetrisWorld(c, l1, l2, m, hold, piece, pile) = world
    val ((x, y), s) = world.piece
    list match {
      case Nil => A.WellHeight
      case n::ns =>
        if (collision(TetrisWorld(c, l1, l2, m, hold, ((x, n), s), pile))) n
        else colly(world, ns)
    }
  }

}

// ゲームの実行
object A extends App {
  // ゲームウィンドウとブロックのサイズ
  val WellWidth = 10
  val WellHeight = 20
  val BlockSize = 35
  val GameWidth = WellWidth * BlockSize
  val GameHeight = WellHeight * BlockSize
  val counter = S.allShapes.length - 1


  def newPiece(i: Int, l: List[S.Shape]): ((Int, Int), S.Shape) = {
    val pos = (WellWidth / 2 - 1, 0)
    (pos, l(i))
  }

  // 最初のテトロミノ
  val firstMinos = S.random()
  var secondMinos = S.random()
  
  if (firstMinos.last == secondMinos.head) {
    secondMinos = secondMinos.reverse
  }

  val piece = newPiece(0, firstMinos)

  // ゲームの初期値
  val world = TetrisWorld(0, firstMinos, secondMinos, 0, Nil, piece, List.fill(WellHeight)(List.fill(WellWidth)(Transparent)))

  // 音楽再生 音声ファイルは https://fc.sitefactory.info/
  // https://www.otosozai.com/ より
  // Macで動かす場合はパス文字列を変更すること
  val bgm = new Sound(getClass.getResourceAsStream("/korobshka.wav"))
  val se1 = new Sound(getClass.getResourceAsStream("/se_saa04.wav"))
  val se2 = new Sound(getClass.getResourceAsStream("/se_sad04.wav"))
  val se3 = new Sound(getClass.getResourceAsStream("/se_ymd08.wav"))
  bgm.playloop()
  
  // ゲームの開始
  world.bigBang(GameWidth + BlockSize * 6, GameHeight, 6 * A.BlockSize / 7, 1)
}
