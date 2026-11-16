import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen, waitFor, cleanup } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import axios from 'axios'
import Booking from './Booking'

vi.mock('axios')

const NEXT_MONTH_LABEL = 'החודש הבא'
const PREVIOUS_MONTH_LABEL = 'החודש הקודם'
const MID_MONTH_DAY = 15
const FIRST_NAME_REQUIRED_MESSAGE = 'יש למלא שם פרטי.'
const EMAIL_INVALID_MESSAGE = 'כתובת האימייל לא תקינה.'
const SLOT_TAKEN_MESSAGE = 'המשבצת הזו כבר נתפסה. בחרו שעה אחרת מהרשימה.'
const CONFIRMATION_TITLE = 'ההזמנה אושרה'
const BOOK_ANOTHER_LABEL = 'הזמנה נוספת'
const TRY_AGAIN_MESSAGE = 'ההזמנה לא נשמרה, נסו שוב.'
const TRY_AGAIN_CODE = 'TRY_AGAIN'
const SERVER_REASON = 'client.firstName: must not be blank'
const ROOM = {
    id: 1, name: 'Vault', capacity: 4, minParticipants: 2, durationMinutes: 60, description: 'Locked',
    prices: [{ fromParticipants: 2, pricePerParticipant: 170, discountedPrice: 170 }, { fromParticipants: 3, pricePerParticipant: 150, discountedPrice: 140 }],
}
const FREE_SLOT = { startAt: '2099-01-01T10:00:00', endAt: '2099-01-01T11:00:00', available: true }
const TAKEN_SLOT = { startAt: '2099-01-01T12:00:00', endAt: '2099-01-01T13:00:00', available: false }
const AVAILABLE_LABEL = 'פנוי'
const BOOKED_LABEL = 'תפוס'
const SUBMIT_LABEL = 'אשר הזמנה'
const CANCEL_LABEL = 'ביטול'
const DIALOG_TITLE = 'הזמנת משחק'
const FIRST_NAME_LABEL = 'שם פרטי'
const LAST_NAME_LABEL = 'שם משפחה'
const PHONE_LABEL = 'טלפון'
const EMAIL_LABEL = /אימייל/
const FIRST_NAME = 'Dana'
const LAST_NAME = 'Levi'
const PHONE = '0501234567'

const rejectedWith = (status, extraData = {}) => ({ response: { status, data: { detail: SERVER_REASON, ...extraData } } })

const showBookingPage = () => render(<MemoryRouter><Booking /></MemoryRouter>)

const availabilityCalls = () => axios.get.mock.calls.filter(([url]) => url.includes('/availability')).length

const openFirstFreeSlot = async user => {
    const freeSlots = await screen.findAllByRole('button', { name: new RegExp(AVAILABLE_LABEL) })
    await user.click(freeSlots[0])
}

const fillClientDetails = async user => {
    await user.type(screen.getByLabelText(FIRST_NAME_LABEL), FIRST_NAME)
    await user.type(screen.getByLabelText(LAST_NAME_LABEL), LAST_NAME)
    await user.type(screen.getByLabelText(PHONE_LABEL), PHONE)
}

describe('Booking page', () => {
    beforeEach(() => {
        axios.get.mockImplementation(url =>
            Promise.resolve({ data: url.endsWith('/api/rooms') ? [ROOM] : [FREE_SLOT, TAKEN_SLOT] }))
        vi.spyOn(console, 'error').mockImplementation(() => {})
    })

    afterEach(() => {
        cleanup()
        vi.restoreAllMocks()
    })

    it('shows the available and taken slots of today when the page opens', async () => {
        showBookingPage()

        expect(await screen.findAllByText(AVAILABLE_LABEL)).toHaveLength(1)
        expect(screen.getAllByText(BOOKED_LABEL)).toHaveLength(1)
        expect(screen.getByRole('button', { name: new RegExp(BOOKED_LABEL) })).toBeDisabled()
        expect(screen.getByRole('button', { name: new RegExp(AVAILABLE_LABEL) })).toBeEnabled()
        const [, { params }] = axios.get.mock.calls.find(([url]) => url.includes('/availability'))
        expect(params.date).toBe(new Date().toLocaleDateString('en-CA'))
    })

    it('loads the slots of a date the customer picks in another month', async () => {
        const user = userEvent.setup()
        showBookingPage()
        await screen.findAllByText(AVAILABLE_LABEL)
        const today = new Date()
        const pickedDay = new Date(today.getFullYear(), today.getMonth() + 1, MID_MONTH_DAY)

        await user.click(screen.getByRole('button', { name: NEXT_MONTH_LABEL }))
        await user.click(screen.getByRole('button', { name: new RegExp(`${MID_MONTH_DAY} ב`) }))

        await waitFor(() => {
            const dates = axios.get.mock.calls
                .filter(([url]) => url.includes('/availability'))
                .map(([, { params }]) => params.date)
            expect(dates).toContain(pickedDay.toLocaleDateString('en-CA'))
        })
    })

    it('offers dates from today up to three months ahead and no further', async () => {
        const user = userEvent.setup()
        showBookingPage()
        await screen.findAllByText(AVAILABLE_LABEL)

        expect(screen.getByRole('button', { name: PREVIOUS_MONTH_LABEL })).toBeDisabled()
        for (let month = 0; month < 3; month++) {
            await user.click(screen.getByRole('button', { name: NEXT_MONTH_LABEL }))
        }

        expect(screen.getByRole('button', { name: NEXT_MONTH_LABEL })).toBeDisabled()
    })

    it('tells the customer when the chosen date has no free slot', async () => {
        axios.get.mockImplementation(url =>
            Promise.resolve({ data: url.endsWith('/api/rooms') ? [ROOM] : [TAKEN_SLOT] }))

        showBookingPage()

        expect(await screen.findByText(/אין שעות פנויות בתאריך הזה/)).toBeInTheDocument()
    })

    it('numbers the steps: room, date and time on the page, then the details in the dialog', async () => {
        const user = userEvent.setup()
        showBookingPage()
        await screen.findAllByText(AVAILABLE_LABEL)

        expect(screen.getByRole('heading', { name: /בחרו חדר/ })).toBeInTheDocument()
        expect(screen.getByRole('heading', { name: /בחרו תאריך/ })).toBeInTheDocument()
        expect(screen.getByRole('heading', { name: /בחרו שעה/ })).toBeInTheDocument()

        await openFirstFreeSlot(user)

        expect(screen.getByText('הפרטים שלכם')).toBeInTheDocument()
    })

    it('shows a loading message, not a connection error, while the rooms are loading', () => {
        axios.get.mockReturnValue(new Promise(() => {}))

        showBookingPage()

        expect(screen.getByText('טוענים את החדרים…')).toBeInTheDocument()
        expect(screen.queryByText(/לא ניתן להתחבר לשרת/)).not.toBeInTheDocument()
    })

    it('shows a connection error when the rooms cannot be loaded', async () => {
        axios.get.mockRejectedValue(new Error('network down'))

        showBookingPage()

        expect(await screen.findByText(/לא ניתן להתחבר לשרת/)).toBeInTheDocument()
    })

    it('names the empty required field next to it, focuses it and sends nothing', async () => {
        const user = userEvent.setup()
        showBookingPage()
        await openFirstFreeSlot(user)

        await user.click(screen.getByRole('button', { name: SUBMIT_LABEL }))

        expect(screen.getByText(FIRST_NAME_REQUIRED_MESSAGE)).toBeInTheDocument()
        expect(screen.getByLabelText(FIRST_NAME_LABEL)).toHaveFocus()
        expect(screen.getByLabelText(FIRST_NAME_LABEL)).toBeInvalid()
        expect(axios.post).not.toHaveBeenCalled()
    })

    it('refuses a malformed email before calling the server', async () => {
        const user = userEvent.setup()
        showBookingPage()
        await openFirstFreeSlot(user)
        await fillClientDetails(user)
        await user.type(screen.getByLabelText(EMAIL_LABEL), 'not-an-email')

        await user.click(screen.getByRole('button', { name: SUBMIT_LABEL }))

        expect(screen.getByText(EMAIL_INVALID_MESSAGE)).toBeInTheDocument()
        expect(axios.post).not.toHaveBeenCalled()
    })

    it('opens a labelled dialog with focus on the first field, and Escape closes it and returns focus', async () => {
        const user = userEvent.setup()
        showBookingPage()
        const freeSlots = await screen.findAllByRole('button', { name: new RegExp(AVAILABLE_LABEL) })
        await user.click(freeSlots[0])

        const dialog = screen.getByRole('dialog', { name: DIALOG_TITLE })
        expect(dialog).toHaveAttribute('aria-modal', 'true')
        expect(screen.getByLabelText(FIRST_NAME_LABEL)).toHaveFocus()

        await user.keyboard('{Escape}')

        expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
        expect(freeSlots[0]).toHaveFocus()
    })

    it('keeps Tab inside the dialog', async () => {
        const user = userEvent.setup()
        showBookingPage()
        await openFirstFreeSlot(user)

        await user.click(screen.getByRole('button', { name: CANCEL_LABEL }))
        await openFirstFreeSlot(user)
        screen.getByRole('button', { name: CANCEL_LABEL }).focus()
        await user.tab()

        expect(screen.getByLabelText(FIRST_NAME_LABEL)).toHaveFocus()
    })

    it('sends the booking and replaces the page with the confirmation of that game', async () => {
        axios.post.mockResolvedValue({})
        const user = userEvent.setup()
        showBookingPage()
        await openFirstFreeSlot(user)
        await fillClientDetails(user)
        const callsBeforeSubmit = availabilityCalls()

        await user.click(screen.getByRole('button', { name: SUBMIT_LABEL }))

        expect(await screen.findByRole('heading', { name: CONFIRMATION_TITLE })).toHaveFocus()
        expect(screen.getByText(ROOM.name)).toBeInTheDocument()
        expect(screen.getByText('10:00-11:00')).toBeInTheDocument()
        expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
        const [, body] = axios.post.mock.calls[0]
        expect(body).toMatchObject({
            roomId: ROOM.id,
            startAt: FREE_SLOT.startAt,
            client: { firstName: FIRST_NAME, lastName: LAST_NAME, phoneNumber: PHONE },
        })

        await user.click(screen.getByRole('button', { name: BOOK_ANOTHER_LABEL }))

        expect(await screen.findAllByText(AVAILABLE_LABEL)).toHaveLength(1)
        expect(screen.queryByRole('heading', { name: CONFIRMATION_TITLE })).not.toBeInTheDocument()
        expect(availabilityCalls()).toBeGreaterThan(callsBeforeSubmit)
    })

    it('shows the total price before confirming and again on the confirmation', async () => {
        axios.post.mockResolvedValue({})
        const user = userEvent.setup()
        showBookingPage()
        await openFirstFreeSlot(user)
        const defaultTotal = '340 ₪'

        expect(screen.getByText(defaultTotal)).toBeInTheDocument()

        await fillClientDetails(user)
        await user.click(screen.getByRole('button', { name: SUBMIT_LABEL }))
        await screen.findByRole('heading', { name: CONFIRMATION_TITLE })

        expect(screen.getByText(defaultTotal)).toBeInTheDocument()
    })

    it('uses the price tier of the group size, and offers only sizes the room accepts', async () => {
        const user = userEvent.setup()
        showBookingPage()
        await openFirstFreeSlot(user)

        await user.selectOptions(screen.getByLabelText('מספר משתתפים'), '3')

        expect(screen.getByText('450 ₪')).toBeInTheDocument()
        expect(screen.getAllByRole('option', { name: /משתתפים/ }).map(option => option.value)).toEqual(['2', '3', '4'])
    })

    it('shows no total for a room that has no price tiers', async () => {
        axios.get.mockImplementation(url =>
            Promise.resolve({ data: url.endsWith('/api/rooms') ? [{ ...ROOM, prices: [] }] : [FREE_SLOT, TAKEN_SLOT] }))
        const user = userEvent.setup()
        showBookingPage()

        await openFirstFreeSlot(user)

        expect(screen.getByRole('dialog')).toBeInTheDocument()
        expect(screen.queryByText(/סה״כ לתשלום/)).not.toBeInTheDocument()
    })

    it('shows the server reason when the server answers 400', async () => {
        axios.post.mockRejectedValue(rejectedWith(400))
        const user = userEvent.setup()
        showBookingPage()
        await openFirstFreeSlot(user)
        await fillClientDetails(user)

        await user.click(screen.getByRole('button', { name: SUBMIT_LABEL }))

        expect(await screen.findByText(SERVER_REASON)).toBeInTheDocument()
        expect(screen.getByRole('dialog')).toBeInTheDocument()
    })

    it('asks the customer to try again on a 409 that is not a taken slot, and keeps the form', async () => {
        axios.post.mockRejectedValue(rejectedWith(409, { code: TRY_AGAIN_CODE }))
        const user = userEvent.setup()
        showBookingPage()
        await openFirstFreeSlot(user)
        await fillClientDetails(user)
        const callsBeforeSubmit = availabilityCalls()

        await user.click(screen.getByRole('button', { name: SUBMIT_LABEL }))

        expect(await screen.findByText(TRY_AGAIN_MESSAGE)).toBeInTheDocument()
        expect(availabilityCalls()).toBe(callsBeforeSubmit)
        expect(screen.getByRole('button', { name: SUBMIT_LABEL })).toBeInTheDocument()
    })

    it('tells the customer the slot is taken on 409 and reloads the day', async () => {
        axios.post.mockRejectedValue(rejectedWith(409))
        const user = userEvent.setup()
        showBookingPage()
        await openFirstFreeSlot(user)
        await fillClientDetails(user)
        const callsBeforeSubmit = availabilityCalls()

        await user.click(screen.getByRole('button', { name: SUBMIT_LABEL }))

        expect(await screen.findByText(SLOT_TAKEN_MESSAGE)).toBeInTheDocument()
        expect(availabilityCalls()).toBeGreaterThan(callsBeforeSubmit)
        expect(screen.queryByRole('button', { name: SUBMIT_LABEL })).not.toBeInTheDocument()
    })
})
