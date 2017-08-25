import React from 'react';
import { graphql, createFragmentContainer } from 'react-relay';

class PlayerOptions extends React.Component {

		render() {
				return (
						<div>
							<label>
								Player:
								<select>
									{this.props.data.map((player, i) => <option key={i} value={player.id}>{player.name}</option>)}
				        </select>
							</label>
						</div>
				);
		}
}

export default createFragmentContainer(PlayerOptions, graphql`
fragment PlayerOptions on Player @relay(plural: true) { id, name }`
);
