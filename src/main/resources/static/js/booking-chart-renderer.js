document.addEventListener('DOMContentLoaded', function() {
    const chart = document.querySelector('.booking-chart');
    if (!chart) return;

    const cellWidth = chart.querySelector('.booking-cell')?.offsetWidth || 40;

    // Проходим по каждой строке с данными
    chart.querySelectorAll('tr.room-data-row').forEach(row => {
        let processedBookings = new Set();

        row.querySelectorAll('td.booking-cell').forEach(cell => {
            const bookingId = cell.dataset.bookingId;

            // Если в ячейке есть бронь и мы ее еще не отрисовали
            if (bookingId && !processedBookings.has(bookingId)) {
                renderBookingBlock(bookingId, row, cellWidth);
                processedBookings.add(bookingId);
            }
        });
    });
});

function renderBookingBlock(bookingId, row, cellWidth) {
    const bookingCells = Array.from(row.querySelectorAll(`td[data-booking-id="${bookingId}"]`));
    if (bookingCells.length === 0) return;

    const firstCell = bookingCells[0];
    const guestName = firstCell.dataset.guestName;
    const status = firstCell.dataset.status;

    // Создаем DOM-элемент блока бронирования
    const block = document.createElement('div');
    block.className = 'booking-block';
    if (status) {
        block.classList.add(`status-${status.toLowerCase()}`);
    }
    block.setAttribute('title', guestName); // Для всплывающей подсказки

    const nameSpan = document.createElement('span');
    nameSpan.className = 'guest-name';
    nameSpan.textContent = guestName;
    block.appendChild(nameSpan);

    // Вычисляем ширину и позицию блока
    // Ширина = кол-во ячеек * ширину ячейки, минус отступы
    const blockWidth = bookingCells.length * cellWidth - 2; // -2px для учета границ
    block.style.width = `${blockWidth}px`;

    // Позиционируем блок внутри первой ячейки
    firstCell.appendChild(block);
}
