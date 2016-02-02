package ddiehl.batchuninstaller.model

data class App(
    val name: CharSequence,
    var size: Long,
    val packageName: String) {

  override fun toString(): String {
    return "" +
        name + " - " + size
  }
}