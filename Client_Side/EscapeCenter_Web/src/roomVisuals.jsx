import { Anchor, Building2, DoorOpen, Gamepad2, Infinity as InfinityIcon, Landmark, Pill, Search } from 'lucide-react';

// One drawn icon per room, so the pages do not depend on emoji that look different on every device.
const ROOM_ICONS = {
    'אחוזת השכן': Building2,
    'מקדש הקאמי': Landmark,
    'ההתערבות': Search,
    'אינפיניטי': InfinityIcon,
    'נרקוס': Pill,
    'משחק מכור': Gamepad2,
    'דו-קרב צוללות': Anchor,
};

export const RoomIcon = ({ name, ...iconProps }) => {
    const Icon = ROOM_ICONS[name] || DoorOpen;
    return <Icon aria-hidden="true" {...iconProps} />;
};
