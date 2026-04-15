package ru.kode.epub.core.routing

import ru.kode.epub.core.ui.screen.ViewModel

interface Node

interface Flow : Node {
  val component: FlowNavigationComponent<*, *>
}

interface Screen : Node {
  val viewModel: ViewModel<*, *>
}
