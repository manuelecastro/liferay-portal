import React from 'react';
import {createRoot} from 'react-dom/client';

import Captcha from './components/Captcha';

const App = () => {
	return (
		<div>
			<Captcha/>
		</div>
	);
};

class WebComponent extends HTMLElement {
	connectedCallback() {
		this.root = createRoot(this);

		this.root.render(<App/>, this);
	}

	disconnectedCallback() {
		this.root.unmount();

		delete this.root;
	}
}

const ELEMENT_ID = 'liferay-sample-captcha-custom-element';

if (!customElements.get(ELEMENT_ID)) {
	customElements.define(ELEMENT_ID, WebComponent);
}