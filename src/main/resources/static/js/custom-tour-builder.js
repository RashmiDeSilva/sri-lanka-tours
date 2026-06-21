(function () {
    const memberFields = ['maleAdults', 'femaleAdults', 'boys', 'girls', 'seniors', 'infants'];
    const destinationCheckboxSelector = 'input[name="destinationIds"]';

    const fallbackImage = 'https://placehold.co/300x300/e2e8f0/475569?text=SL';

    function parseNumber(value) {
        const parsed = parseInt(value, 10);
        return Number.isFinite(parsed) && parsed > 0 ? parsed : 0;
    }

    function formatMoney(value) {
        return `Rs. ${Number(value || 0).toFixed(2)}`;
    }

    function escapeHtml(value) {
        return String(value ?? '')
            .replaceAll('&', '&amp;')
            .replaceAll('<', '&lt;')
            .replaceAll('>', '&gt;')
            .replaceAll('"', '&quot;')
            .replaceAll("'", '&#39;');
    }

    function getSelectedRadio(name) {
        return document.querySelector(`input[name="${name}"]:checked`);
    }

    function getInputValue(id) {
        const input = document.getElementById(id);
        return input ? parseNumber(input.value) : 0;
    }

    function setInputValue(id, nextValue) {
        const input = document.getElementById(id);
        if (!input) {
            return;
        }
        input.value = Math.max(0, nextValue);
    }

    function formatDate(date) {
        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, '0');
        const day = String(date.getDate()).padStart(2, '0');
        return `${year}-${month}-${day}`;
    }

    function humanDate(value) {
        if (!value) {
            return '';
        }
        const date = new Date(`${value}T00:00:00`);
        if (Number.isNaN(date.getTime())) {
            return '';
        }
        return date.toLocaleDateString(undefined, {
            day: 'numeric',
            month: 'short',
            year: 'numeric'
        });
    }

    function updateTripDates() {
        const startDateInput = document.getElementById('startDate');
        const plannedDaysInput = document.getElementById('plannedDays');
        const endDateInput = document.getElementById('endDate');

        if (!startDateInput || !plannedDaysInput || !endDateInput) {
            return { days: 0, start: '', end: '' };
        }

        const startValue = startDateInput.value;
        const plannedDays = parseNumber(plannedDaysInput.value);

        if (startValue && plannedDays > 0) {
            const startDate = new Date(`${startValue}T00:00:00`);
            if (!Number.isNaN(startDate.getTime())) {
                const endDate = new Date(startDate);
                endDate.setDate(startDate.getDate() + plannedDays - 1);
                endDateInput.value = formatDate(endDate);
                return { days: plannedDays, start: startValue, end: endDateInput.value };
            }
        }

        endDateInput.value = '';
        return { days: plannedDays, start: startValue, end: '' };
    }

    function buildDestinationChip(checkbox) {
        const name = checkbox.dataset.name || checkbox.getAttribute('data-name') || 'Destination';
        const location = checkbox.dataset.location || checkbox.getAttribute('data-location') || '';
        const fee = Number.parseFloat(checkbox.dataset.fee || checkbox.getAttribute('data-fee') || '0') || 0;
        const image = checkbox.dataset.image || checkbox.getAttribute('data-image') || fallbackImage;

        return `
            <div class="flex items-center gap-3 rounded-2xl border border-slate-200 bg-white px-3 py-2 shadow-sm">
                <img src="${escapeHtml(image)}" alt="${escapeHtml(name)}" class="h-10 w-10 rounded-xl object-cover">
                <div class="min-w-0 flex-1">
                    <p class="truncate text-sm font-bold text-slate-900">${escapeHtml(name)}</p>
                    <p class="truncate text-xs font-medium text-slate-500">${escapeHtml(location)}</p>
                </div>
                <span class="whitespace-nowrap text-sm font-black text-blue-700">${formatMoney(fee)}</span>
            </div>
        `;
    }

    function updateDestinationSummary() {
        const selected = Array.from(document.querySelectorAll(`${destinationCheckboxSelector}:checked`));
        const chipsContainer = document.getElementById('selectedDestinationChips');
        const sidebarDestinations = document.getElementById('sidebarDestinations');

        if (!chipsContainer || !sidebarDestinations) {
            return 0;
        }

        if (selected.length === 0) {
            const emptyMarkup = '<p class="text-sm text-slate-500">No destinations selected yet.</p>';
            chipsContainer.innerHTML = emptyMarkup;
            sidebarDestinations.innerHTML = emptyMarkup;
            return 0;
        }

        const html = selected.map(buildDestinationChip).join('');
        chipsContainer.innerHTML = html;
        sidebarDestinations.innerHTML = html;
        return selected.reduce((sum, checkbox) => {
            const fee = Number.parseFloat(checkbox.dataset.fee || checkbox.getAttribute('data-fee') || '0') || 0;
            return sum + fee;
        }, 0);
    }

    function updateMemberSummary() {
        const maleAdults = getInputValue('maleAdults');
        const femaleAdults = getInputValue('femaleAdults');
        const boys = getInputValue('boys');
        const girls = getInputValue('girls');
        const seniors = getInputValue('seniors');
        const infants = getInputValue('infants');

        const adults = maleAdults + femaleAdults;
        const children = boys + girls;
        const optional = seniors + infants;
        const total = adults + children + optional;

        const summaryAdults = document.getElementById('summaryAdults');
        const summaryChildren = document.getElementById('summaryChildren');
        const summaryOptional = document.getElementById('summaryOptional');
        const summaryTotal = document.getElementById('summaryTotalMembers');
        const sidebarMembers = document.getElementById('sidebarMembers');

        if (summaryAdults) summaryAdults.textContent = adults;
        if (summaryChildren) summaryChildren.textContent = children;
        if (summaryOptional) summaryOptional.textContent = optional;
        if (summaryTotal) summaryTotal.textContent = total;
        if (sidebarMembers) {
            sidebarMembers.textContent = `${adults} adults, ${children} children, ${optional} optional travelers, ${total} total`;
        }

        return { adults, children, optional, total };
    }

    function updateGuideSummary() {
        const selectedGuide = getSelectedRadio('guideId');
        const sidebarGuide = document.getElementById('sidebarGuide');

        if (!sidebarGuide) {
            return { pricePerDay: 0, name: '' };
        }

        if (!selectedGuide) {
            sidebarGuide.textContent = 'No guide selected yet.';
            return { pricePerDay: 0, name: '' };
        }

        const name = selectedGuide.dataset.name || 'Guide';
        const email = selectedGuide.dataset.email || '';
        const pricePerDay = Number.parseFloat(selectedGuide.dataset.price || '0') || 0;
        const languages = selectedGuide.dataset.languages || '';

        sidebarGuide.innerHTML = `
            <div class="space-y-1">
                <p class="font-bold text-slate-900">${escapeHtml(name)}</p>
                <p class="text-xs text-slate-500">${escapeHtml(email)}</p>
                <p class="text-xs text-slate-500">${escapeHtml(languages ? `Languages: ${languages}` : '')}</p>
                <p class="text-xs font-semibold text-blue-700">${formatMoney(pricePerDay)} per day</p>
            </div>
        `;

        return { pricePerDay, name, email, languages };
    }

    function updateTouristNoteSummary() {
        const noteInput = document.getElementById('touristNote');
        const sidebarNote = document.getElementById('sidebarNote');

        if (!noteInput || !sidebarNote) {
            return '';
        }

        const selectedGuide = getSelectedRadio('guideId');
        const currentNote = noteInput.value.trim();

        if (selectedGuide) {
            const guideName = selectedGuide.dataset.name || 'selected guide';
            const autoNote = `Booked guide: ${guideName}.`;
            if (!currentNote || currentNote.startsWith('Booked guide:')) {
                noteInput.value = autoNote;
                noteInput.dataset.autoNote = autoNote;
            }
        }

        const resolvedNote = noteInput.value.trim();
        sidebarNote.textContent = resolvedNote || 'Add a note for your selected guide.';
        return resolvedNote;
    }

    function updateCostSummary(days, destinationFee, guidePricePerDay) {
        const guideFee = (guidePricePerDay || 0) * days;
        const total = guideFee + destinationFee;

        const reviewDays = document.getElementById('reviewDays');
        const reviewGuideFee = document.getElementById('reviewGuideFee');
        const reviewDestFee = document.getElementById('reviewDestFee');
        const reviewTotalFee = document.getElementById('reviewTotalFee');
        const sidebarCost = document.getElementById('sidebarCost');

        if (reviewDays) reviewDays.textContent = days;
        if (reviewGuideFee) reviewGuideFee.textContent = formatMoney(guideFee);
        if (reviewDestFee) reviewDestFee.textContent = formatMoney(destinationFee);
        if (reviewTotalFee) reviewTotalFee.textContent = formatMoney(total);
        if (sidebarCost) sidebarCost.textContent = formatMoney(total);

        return { guideFee, total };
    }

    function updateTripSummary() {
        const trip = updateTripDates();
        const memberSummary = updateMemberSummary();
        const destinationFee = updateDestinationSummary();
        const guideSummary = updateGuideSummary();
        updateTouristNoteSummary();

        const sidebarTripDates = document.getElementById('sidebarTripDates');
        if (sidebarTripDates) {
            if (trip.start && trip.end) {
                const nights = Math.max(trip.days - 1, 0);
                sidebarTripDates.textContent = `${humanDate(trip.start)} - ${humanDate(trip.end)} | ${trip.days} day(s) / ${nights} night(s)`;
            } else if (trip.start) {
                sidebarTripDates.textContent = `${humanDate(trip.start)} | choose the trip days`;
            } else {
                sidebarTripDates.textContent = 'Pick a start date and trip length.';
            }
        }

        updateCostSummary(trip.days, destinationFee, guideSummary.pricePerDay);

        if (typeof lucide !== 'undefined' && typeof lucide.createIcons === 'function') {
            lucide.createIcons();
        }
    }

    function validateTourForm(event) {
        const selectedDestinations = document.querySelectorAll(`${destinationCheckboxSelector}:checked`);
        const startDateInput = document.getElementById('startDate');
        const plannedDaysInput = document.getElementById('plannedDays');
        const endDateInput = document.getElementById('endDate');
        const guide = getSelectedRadio('guideId');
        const noteInput = document.getElementById('touristNote');

        const memberSummary = updateMemberSummary();
        updateTripDates();

        if (selectedDestinations.length === 0) {
            alert('Please select at least one destination.');
            event.preventDefault();
            return false;
        }

        if (!startDateInput.value || !plannedDaysInput.value || parseNumber(plannedDaysInput.value) <= 0 || !endDateInput.value) {
            alert('Please choose a start date and trip length.');
            event.preventDefault();
            return false;
        }

        if (memberSummary.total <= 0) {
            alert('Please enter at least one traveler.');
            event.preventDefault();
            return false;
        }

        if (!guide) {
            alert('Please select a guide.');
            event.preventDefault();
            return false;
        }

        if (noteInput) {
            updateTouristNoteSummary();
        }

        return true;
    }

    function bindStepperButtons() {
        document.querySelectorAll('[data-stepper-button]').forEach((button) => {
            button.addEventListener('click', () => {
                const targetId = button.dataset.target;
                const direction = parseNumber(button.dataset.direction || '0') || 0;
                const input = document.getElementById(targetId);

                if (!input) {
                    return;
                }

                const currentValue = parseNumber(input.value);
                input.value = Math.max(0, currentValue + direction);
                updateTripSummary();
            });
        });
    }

    function bindLiveUpdates() {
        document.addEventListener('change', (event) => {
            if (
                event.target.matches(destinationCheckboxSelector) ||
                event.target.matches('input[name="guideId"]') ||
                event.target.id === 'startDate' ||
                event.target.id === 'plannedDays' ||
                memberFields.includes(event.target.id) ||
                event.target.id === 'touristNote'
            ) {
                updateTripSummary();
            }
        });

        document.addEventListener('input', (event) => {
            if (memberFields.includes(event.target.id) || event.target.id === 'startDate' || event.target.id === 'plannedDays' || event.target.id === 'touristNote') {
                if (memberFields.includes(event.target.id)) {
                    event.target.value = String(Math.max(0, parseNumber(event.target.value)));
                }
                updateTripSummary();
            }
        });
    }

    document.addEventListener('DOMContentLoaded', () => {
        const tourForm = document.getElementById('tourForm');
        if (tourForm) {
            tourForm.addEventListener('submit', validateTourForm);
        }

        bindStepperButtons();
        bindLiveUpdates();
        updateTripSummary();
    });
})();
