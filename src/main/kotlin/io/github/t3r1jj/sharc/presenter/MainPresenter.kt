package io.github.t3r1jj.sharc.presenter

import io.github.t3r1jj.sharc.UI
import io.github.t3r1jj.sharc.external.WarshipsAPI
import io.github.t3r1jj.sharc.model.Ship
import kotlin.browser.document

class MainPresenter(private val MAX_SIZE: Int) : ArrayList<Ship>(MAX_SIZE), Presenter {
    var removeButtonClass = "btn btn-danger"
    private val api = WarshipsAPI()

    private var nationSelect: dynamic = UI.NATION_SELECT.getElement()
    private var typeSelect: dynamic = UI.TYPE_SELECT.getElement()
    private var shipSelect: dynamic = UI.SHIP_SELECT.getElement()
    private var shipAddButton: dynamic = UI.SHIP_ADD_BUTTON.getElement()
    private var selectedShipsList: dynamic = UI.SELECTED_SHIPS_LIST.getElement()
    private var rangeSlider: dynamic = UI.RANGE_SLIDER.getElement()
    private var rangeLabel: dynamic = UI.RANGE_LABEL.getElement()
    private var maxRangeLabel: dynamic = UI.MAX_RANGE_LABEL.getElement()
    private var chartDiv: dynamic = UI.CHART_DIV.getElement()

    private val subPresenters = arrayOf(
            InfoPresenter(UI.INFO_TABLE),
            ArcsChartPresenter(UI.SHELL_ARCS_CHART),
            TimeChartPresenter(UI.SHELL_TIME_CHART))

    init {
        initializeBasicInfo()
        nationSelect.onchange = { loadShips {} }
        typeSelect.onchange = { loadShips {} }
        shipAddButton.onclick = {
            shipAddButton.disabled = true
            addShip {}
        }
        rangeSlider.oninput = { reloadView(this, getRange()) }
    }

    private fun addShip(callback: (Boolean) -> Unit) {
        val selectedShip = getSelectedShip()
        selectedShip?.let {
            if (!this.contains(selectedShip) && this.size < MAX_SIZE) {
                api.loadShip(selectedShip, {
                    loadShip(selectedShip)
                    callback(true)
                })
            } else {
                callback(false)
            }
        }
    }

    private fun loadShip(selectedShip: Ship) {
        val listItem = document.createElement("li").asDynamic()
        listItem.id = selectedShip.id
        selectedShip.icon?.let {
            val img = document.createElement("img");
            img.setAttribute("src", selectedShip.icon!!)
            img.setAttribute("alt", selectedShip.name + "-wows-wg-icon")
            img.setAttribute("height", "38px")
            listItem.appendChild(img)
        }
        listItem.innerHTML += selectedShip.name + " " + selectedShip.nation + " T" + selectedShip.tier + " "
        val removeButton = document.createElement("button").asDynamic()
        removeButton.setAttribute("class", removeButtonClass)
        removeButton.innerText = "X"
        removeButton.onclick = { removeShip(selectedShip.id) }
        listItem.appendChild(removeButton)
        selectedShipsList.appendChild(listItem)
        this.add(selectedShip)
        if (this.size == 1 && (rangeSlider.getAttribute("min") as String).toDouble() == getRange()) {
            reloadRangeSlider()
            rangeSlider.value = getMaxRange()
        }
        selectedShip.initCalculators()
        calculateArcs()
        reloadView(this, getRange())
    }

    private fun removeShip(id: String) {
        this.remove(this.first { ship -> ship.id == id })
        selectedShipsList.removeChild(document.getElementById(id))
        reloadView(this, getRange())
    }

    override fun reloadView(ships: Collection<Ship>, range: Double) {
        hideChartDiv(this.isEmpty())
        reloadRangeSlider()
        subPresenters.forEach { it.reloadView(this, getRange()) }
        shipAddButton.disabled = this.size == MAX_SIZE
    }

    private fun getRange() =
            (rangeSlider.value as String).toDouble()

    private fun hideChartDiv(hide: Boolean) {
        if (hide) {
            chartDiv.classList.remove(UI.CHART_DIV.id + "-visible")
            chartDiv.classList.add(UI.CHART_DIV.id + "-invisible")
        } else {
            chartDiv.classList.remove(UI.CHART_DIV.id + "-invisible")
            chartDiv.classList.add(UI.CHART_DIV.id + "-visible")
        }
    }

    private fun reloadRangeSlider() {
        val maxRange = getMaxRange()
        rangeSlider.setAttribute("max", maxRange)
        val range = getRange()
        if (range > maxRange) {
            rangeSlider.value = maxRange
        }
        rangeLabel.innerText = "Range: " + range.div(1000) + " km"
        maxRangeLabel.innerText = maxRange.div(1000).toString() + " km"
    }

    private fun getMaxRange() = this.maxBy { ship -> ship.getMaxRange() }?.getMaxRange() ?: 0.0


    private fun getSelectedShip(): Ship? {
        return try {
            api.getShip(shipSelect.value)
        } catch (e: NoSuchElementException) {
            null
        }
    }

    private fun initializeBasicInfo() {
        api.loadBasicInfo({
            val shipNations = api.getShipNations()
            for ((nationId, nationText) in shipNations) {
                val option = document.createElement("option").asDynamic()
                option.value = nationId
                option.text = nationText
                nationSelect.add(option)
            }
            val shipTypes = api.getShipTypes()
            for ((typeId, typeText) in shipTypes) {
                val option = document.createElement("option").asDynamic()
                option.value = typeId
                option.text = typeText
                typeSelect.add(option)
            }
            loadShips({})
        })
    }

    private fun calculateArcs() {
        this
                .flatMap { it.shellCalculators.values }
                .filterNot { it.hasData() }
                .forEach { it.calculateArcs() }
    }

    private fun getShipsSelection(): WarshipsAPI.ShipsSelection {
        val selectedNation = nationSelect.value as String
        val selectedType = typeSelect.value as String
        return WarshipsAPI.ShipsSelection(selectedNation, selectedType)
    }

    private fun loadShips(callback: () -> Unit) {
        val shipsSelection = getShipsSelection()
        api.loadShips(shipsSelection, {
            val ships = api.getShips(shipsSelection)
            clearSelect(shipSelect)
            for (i in 0 until ships.size) {
                val option = document.createElement("option").asDynamic()
                option.value = ships[i].id
                option.text = ships[i].getTier() + ". " + ships[i].name
                if (ships[i].isPremium) {
                    option.text += " (P)"
                }
                if (ships[i].isDemo) {
                    option.text += " [Demo]"
                }
                shipSelect.add(option)
            }
            callback()
        })
    }

    private fun clearSelect(select: dynamic) {
        while ((select.options.length as Int) > 0) {
            select.remove(0)
        }
    }

}