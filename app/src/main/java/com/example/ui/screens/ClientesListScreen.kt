package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.ClienteEntity
import com.example.data.model.CreditoEntity
import com.example.ui.components.KwanzaFormatters
import com.example.ui.theme.*
import com.example.ui.viewmodel.SortOption

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientesListScreen(
    clientes: List<Pair<ClienteEntity, CreditoEntity?>>,
    searchQuery: String,
    selectedStateFilter: String,
    selectedBankFilter: String,
    sortOption: SortOption,
    onSearchQueryChange: (String) -> Unit,
    onStateFilterChange: (String) -> Unit,
    onBankFilterChange: (String) -> Unit,
    onSortOptionChange: (SortOption) -> Unit,
    onSelectCliente: (Long) -> Unit,
    onAddClienteClick: () -> Unit,
    onEditCliente: (ClienteEntity) -> Unit = {},
    onDeleteCliente: (ClienteEntity) -> Unit = {}
) {
    var showSortMenu by remember { mutableStateOf(false) }
    var clientToDelete by remember { mutableStateOf<ClienteEntity?>(null) }
    var lembreteTarget by remember { mutableStateOf<Pair<ClienteEntity, CreditoEntity?>?>(null) }

    val stateFilters = listOf("Todos", "Ativo", "Em atraso", "Liquidado", "Renegociado")
    val bankFilters = listOf("Todos", "BPC", "BAI", "BFA", "Banco Económico", "SOL", "Banco Atlântico", "BIC", "BCI", "KEVE", "Standard Bank", "YETU", "Caixa Tota", "Outro")

    clientToDelete?.let { cliente ->
        AlertDialog(
            onDismissRequest = { clientToDelete = null },
            title = { Text("Excluir Cliente", fontWeight = FontWeight.Bold, color = WarningRed) },
            text = { Text("Tem certeza que deseja excluir permanentemente ${cliente.nome}? Todos os empréstimos e registos deste cliente serão removidos.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteCliente(cliente)
                        clientToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WarningRed)
                ) {
                    Text("Excluir", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { clientToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddClienteClick,
                containerColor = DeepBlue,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.testTag("add_cliente_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Novo Cliente")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(GrayBackground)
        ) {
            // Search Bar & Sort Dropdown
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text("Pesquisar por nome, banco, tel, empresa, nº...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = DeepBlue) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Limpar")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_cliente_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DeepBlue,
                        unfocusedBorderColor = GraySurface
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Sort and Bank Selectors
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Sort Menu Button
                    Box {
                        OutlinedButton(
                            onClick = { showSortMenu = true },
                            shape = RoundedCornerShape(20.dp),
                            border = ButtonDefaults.outlinedButtonBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(DeepBlue)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("sort_button")
                        ) {
                            Icon(Icons.Default.Sort, contentDescription = null, modifier = Modifier.size(16.dp), tint = DeepBlue)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Ordem: ${getSortOptionLabel(sortOption)}",
                                fontSize = 12.sp,
                                color = DeepBlue,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            SortOption.values().forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(getSortOptionLabel(option)) },
                                    onClick = {
                                        onSortOptionChange(option)
                                        showSortMenu = false
                                    }
                                )
                            }
                        }
                    }

                    // Total Count Label
                    Text(
                        text = "${clientes.size} cliente(s)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = GrayText
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // State Filter Chips Scrollable
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    stateFilters.forEach { state ->
                        FilterChip(
                            selected = selectedStateFilter == state,
                            onClick = { onStateFilterChange(state) },
                            label = { Text(state, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = DeepBlue,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Bank Filter Chips Scrollable
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    bankFilters.forEach { bank ->
                        FilterChip(
                            selected = selectedBankFilter == bank,
                            onClick = { onBankFilterChange(bank) },
                            label = { Text(bank, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ForestGreen,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }

            // Client Cards List
            if (clientes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.SearchOff,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = GrayText
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Nenhum cliente encontrado",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tente alterar os filtros ou adicione um novo cliente.",
                            fontSize = 13.sp,
                            color = GrayText
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(clientes, key = { it.first.id }) { (cliente, credito) ->
                        ClientListItemCard(
                            cliente = cliente,
                            credito = credito,
                            onClick = { onSelectCliente(cliente.id) },
                            onLembreteClick = { lembreteTarget = Pair(cliente, credito) },
                            onEditClick = { onEditCliente(cliente) },
                            onDeleteClick = { clientToDelete = cliente }
                        )
                    }
                }
            }
        }
    }

    lembreteTarget?.let { (cliente, credito) ->
        com.example.ui.components.EnviarLembreteDialog(
            cliente = cliente,
            credito = credito,
            onDismiss = { lembreteTarget = null }
        )
    }
}

@Composable
fun ClientListItemCard(
    cliente: ClienteEntity,
    credito: CreditoEntity?,
    onClick: () -> Unit,
    onLembreteClick: () -> Unit = {},
    onEditClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {}
) {
    val isAtraso = credito?.estado == "Em atraso" || (credito?.diasEmAtraso ?: 0) > 0
    val isLiquidado = credito?.estado == "Liquidado"

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("cliente_item_${cliente.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(if (isAtraso) WarningRedLight else DeepBlue.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!cliente.fotoUri.isNullOrEmpty()) {
                            AsyncImage(
                                model = cliente.fotoUri,
                                contentDescription = "Foto do Cliente",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Text(
                                text = cliente.nome.take(2).uppercase(),
                                fontWeight = FontWeight.Bold,
                                color = if (isAtraso) WarningRed else DeepBlue,
                                fontSize = 16.sp
                            )
                        }
                    }

                    Column {
                        Text(
                            text = cliente.nome,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${cliente.numeroInterno} • ${cliente.empresa.ifEmpty { "Particular" }}",
                            fontSize = 12.sp,
                            color = GrayText
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = when {
                            isAtraso -> WarningRed
                            isLiquidado -> ForestGreen
                            else -> DeepBlue
                        },
                        contentColor = Color.White
                    ) {
                        Text(
                            text = when {
                                isAtraso -> "Em Atraso (${credito?.diasEmAtraso}d)"
                                isLiquidado -> "Liquidado"
                                else -> "Ativo"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    IconButton(onClick = onLembreteClick, modifier = Modifier.size(32.dp).testTag("btn_item_lembrete_${cliente.id}")) {
                        Icon(Icons.Default.Chat, contentDescription = "Lembrete SMS/WhatsApp", tint = DarkGold, modifier = Modifier.size(18.dp))
                    }

                    IconButton(onClick = onEditClick, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar", tint = DeepBlue, modifier = Modifier.size(18.dp))
                    }

                    IconButton(onClick = onDeleteClick, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Excluir", tint = WarningRed, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = GraySurface)
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Banco / IBAN", fontSize = 11.sp, color = GrayText)
                    Text(
                        text = "${cliente.banco} (${cliente.numeroConta.takeLast(4)})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Column {
                    Text("Desembolsado", fontSize = 11.sp, color = GrayText)
                    Text(
                        text = KwanzaFormatters.formatAOA(credito?.valorDesembolsado ?: 0.0),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("Prestação Mensal", fontSize = 11.sp, color = GrayText)
                    Text(
                        text = KwanzaFormatters.formatAOA(credito?.prestacaoMensal ?: 0.0),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isAtraso) WarningRed else DeepBlue
                    )
                }
            }
        }
    }
}

fun getSortOptionLabel(option: SortOption): String {
    return when (option) {
        SortOption.A_Z -> "A-Z"
        SortOption.Z_A -> "Z-A"
        SortOption.NUMERO -> "Número"
        SortOption.BANCO -> "Banco"
        SortOption.EMPRESA -> "Empresa"
        SortOption.DATA_DESEMBOLSO -> "Data Desembolso"
        SortOption.MAIOR_CREDITO -> "Maior Crédito"
        SortOption.MENOR_CREDITO -> "Menor Crédito"
        SortOption.MAIOR_ATRASO -> "Maior Atraso"
    }
}
