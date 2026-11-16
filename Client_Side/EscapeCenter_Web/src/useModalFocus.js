import { useEffect, useRef } from 'react';

const FOCUSABLE = 'input, select, textarea, button:not([disabled]), a[href]';

/**
 * Keeps keyboard focus inside a dialog while it is open: the first control gets focus, Tab wraps around,
 * Escape closes it, and focus goes back to what opened it. Mount the dialog only while it is open.
 */
export const useModalFocus = (dialogRef, onClose) => {
    const onCloseRef = useRef(onClose);
    useEffect(() => {
        onCloseRef.current = onClose;
    });

    useEffect(() => {
        const dialog = dialogRef.current;
        const opener = document.activeElement;
        const focusable = () => [...dialog.querySelectorAll(FOCUSABLE)];
        focusable()[0]?.focus();

        const handleKeyDown = event => {
            if (event.key === 'Escape') {
                onCloseRef.current();
                return;
            }
            if (event.key !== 'Tab') return;
            const items = focusable();
            const first = items[0];
            const last = items[items.length - 1];
            if (event.shiftKey && document.activeElement === first) {
                event.preventDefault();
                last.focus();
            } else if (!event.shiftKey && document.activeElement === last) {
                event.preventDefault();
                first.focus();
            }
        };
        document.addEventListener('keydown', handleKeyDown);
        return () => {
            document.removeEventListener('keydown', handleKeyDown);
            opener?.focus?.();
        };
    }, [dialogRef]);
};
