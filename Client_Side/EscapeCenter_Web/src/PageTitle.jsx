import React from 'react';

const SITE_NAME = 'EscapeCenter';

// React 19 moves a <title> rendered anywhere into the document head.
const PageTitle = ({ children }) => <title>{children ? `${children} | ${SITE_NAME}` : SITE_NAME}</title>;

export default PageTitle;
