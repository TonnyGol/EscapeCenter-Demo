import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen, waitFor, cleanup } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import axios from 'axios'
import Booking from './Booking'

vi.mock('axios')

const DAYS_IN_WEEK = 7
const MISSING_DETAILS_MESSAGE = 'יש למלא שם פרטי, שם משפחה וטלפון.'
const SLOT_TAKEN_MESSAGE = 'המשבצת הזו כבר נתפסה, בחרו שעה אחרת.'
const BOOKING_SAVED_MESSAGE = 'הזמנה נשמרה בהצלחה!'
const SERVER_REASON = 'client.firstName: must not be blank'
const ROOM = { id: 1, name: 'Vault', capacity: 4, durationMinutes: 60 }
const FREE_SLOT = { startAt: '2099-01-01T10:00:00', endAt: '2099-01-01T11:00:00', available: true }
const TAKEN_SLOT = { startAt: '2099-01-01T12:00:00', endAt: '2099-01-01T13:00:00', available: false }
const AVAILABLE_LABEL = 'פנוי'
const BOOKED_LABEL = 'תפוס'
const SUBMIT_LABEL = 'אשר הזמנה'
const FIRST_NAME_PLACEHOLDER = 'שם פרטי'
const LAST_NAME_PLACEHOLDER = 'שם משפחה'
const PHONE_PLACEHOLDER = 'טלפון'
const FIRST_NAME = 'Dana'
const LAST_NAME = 'Levi'
const PHONE = '0501234567'

const rejectedWith = status => ({ response: { status, data: { detail: SERVER_REASON } } })

const showBookingPage = () => render(<MemoryRouter><Booking /></MemoryRouter>)

const availabilityCalls = () => axios.get.mock.calls.filter(([url]) => url.includes('/availability')).length

const openFirstFreeSlot = async user => {
    const freeSlots = await screen.findAllByRole('button', { name: new RegExp(AVAILABLE_LABEL) })
    await user.click(freeSlots[0])
}

const fillClientDetails = async user => {
    await user.type(screen.getByPlaceholderText(FIRST_NAME_PLACEHOLDER), FIRST_NAME)
    await user.type(screen.getByPlaceholderText(LAST_NAME_PLACEHOLDER), LAST_NAME)
    await user.type(screen.getByPlaceholderText(PHONE_PLACEHOLDER), PHONE)
}

describe('Booking page', () => {
    beforeEach(() => {
        axios.get.mockImplementation(url =>
            Promise.resolve({ data: url.endsWith('/api/rooms') ? [ROOM] : [FREE_SLOT, TAKEN_SLOT] }))
        vi.spyOn(window, 'alert').mockImplementation(() => {})
        vi.spyOn(console, 'error').mockImplementation(() => {})
    })

    afterEach(() => {
        cleanup()
        vi.restoreAllMocks()
    })

    it('shows available and taken slots for every day of the week', async () => {
        showBookingPage()

        expect(await screen.findAllByText(AVAILABLE_LABEL)).toHaveLength(DAYS_IN_WEEK)
        expect(screen.getAllByText(BOOKED_LABEL)).toHaveLength(DAYS_IN_WEEK)
        screen.getAllByRole('button', { name: new RegExp(BOOKED_LABEL) })
            .forEach(button => expect(button).toBeDisabled())
        screen.getAllByRole('button', { name: new RegExp(AVAILABLE_LABEL) })
            .forEach(button => expect(button).toBeEnabled())
    })

    it('shows a connection error when the rooms cannot be loaded', async () => {
        axios.get.mockRejectedValue(new Error('network down'))

        showBookingPage()

        expect(await screen.findByText(/לא ניתן להתחבר לשרת/)).toBeInTheDocument()
    })

    it('refuses to submit while a required field is empty', async () => {
        const user = userEvent.setup()
        showBookingPage()
        await openFirstFreeSlot(user)

        await user.click(screen.getByRole('button', { name: SUBMIT_LABEL }))

        expect(window.alert).toHaveBeenCalledWith(MISSING_DETAILS_MESSAGE)
        expect(axios.post).not.toHaveBeenCalled()
    })

    it('sends the booking and reloads the week when the details are valid', async () => {
        axios.post.mockResolvedValue({})
        const user = userEvent.setup()
        showBookingPage()
        await openFirstFreeSlot(user)
        await fillClientDetails(user)
        const callsBeforeSubmit = availabilityCalls()

        await user.click(screen.getByRole('button', { name: SUBMIT_LABEL }))

        await waitFor(() => expect(window.alert).toHaveBeenCalledWith(BOOKING_SAVED_MESSAGE))
        const [, body] = axios.post.mock.calls[0]
        expect(body).toMatchObject({
            roomId: ROOM.id,
            startAt: FREE_SLOT.startAt,
            client: { firstName: FIRST_NAME, lastName: LAST_NAME, phoneNumber: PHONE },
        })
        expect(availabilityCalls()).toBeGreaterThan(callsBeforeSubmit)
    })

    it('shows the server reason when the server answers 400', async () => {
        axios.post.mockRejectedValue(rejectedWith(400))
        const user = userEvent.setup()
        showBookingPage()
        await openFirstFreeSlot(user)
        await fillClientDetails(user)

        await user.click(screen.getByRole('button', { name: SUBMIT_LABEL }))

        await waitFor(() => expect(window.alert).toHaveBeenCalledWith(expect.stringContaining(SERVER_REASON)))
    })

    it('tells the customer the slot is taken on 409 and reloads the week', async () => {
        axios.post.mockRejectedValue(rejectedWith(409))
        const user = userEvent.setup()
        showBookingPage()
        await openFirstFreeSlot(user)
        await fillClientDetails(user)
        const callsBeforeSubmit = availabilityCalls()

        await user.click(screen.getByRole('button', { name: SUBMIT_LABEL }))

        await waitFor(() => expect(window.alert).toHaveBeenCalledWith(SLOT_TAKEN_MESSAGE))
        expect(availabilityCalls()).toBeGreaterThan(callsBeforeSubmit)
        expect(screen.queryByRole('button', { name: SUBMIT_LABEL })).not.toBeInTheDocument()
    })
})
